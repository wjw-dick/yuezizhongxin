package com.yzz.common.storage;

import com.yzz.common.api.BusinessException;
import com.yzz.common.api.ErrorCode;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * S3 兼容存储实现（AWS SigV4），一套代码覆盖 AWS S3、MinIO、阿里云 OSS、腾讯云 COS。
 *
 * <p>对象键规则与本地驱动一致：{@code tenants/{租户标识}/...}，跨租户键直接拒绝；
 * 签名地址用于私有桶临时预览，过期时间由调用方控制。</p>
 */
public class S3CompatibleStorageService implements StorageService {

    private static final String ALGORITHM = "AWS4-HMAC-SHA256";
    private static final DateTimeFormatter AMZ_DATE =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");
    private static final DateTimeFormatter DATE_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String EMPTY_SHA256 =
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

    private final StorageSettings settings;
    private final String endpoint;
    private final String bucket;
    private final int maxSizeMb;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public S3CompatibleStorageService(StorageSettings settings, int maxSizeMb) {
        this.settings = settings;
        this.maxSizeMb = maxSizeMb;
        String ep = settings.getEndpoint();
        if (ep == null || ep.isBlank()) {
            throw BusinessException.of(ErrorCode.STORAGE_ERROR, "对象存储服务地址未配置");
        }
        this.endpoint = ep.replaceAll("/+$", "");
        this.bucket = settings.getBucket() == null ? "" : settings.getBucket().trim();
        if (bucket.isBlank()) {
            throw BusinessException.of(ErrorCode.STORAGE_ERROR, "对象存储 Bucket 未配置");
        }
    }

    @Override
    public String driver() {
        return settings.getDriver();
    }

    @Override
    public StoredFile upload(String tenantCode, String dir, String filename,
                             InputStream inputStream, long size, String contentType) {
        requireTenant(tenantCode);
        if (size > (long) maxSizeMb * 1024 * 1024) {
            throw BusinessException.of(ErrorCode.BAD_REQUEST, "文件超过大小上限 " + maxSizeMb + " MB");
        }
        byte[] body;
        try {
            body = inputStream.readAllBytes();
        } catch (IOException e) {
            throw BusinessException.of(ErrorCode.STORAGE_ERROR, "读取上传内容失败");
        }
        String key = buildKey(tenantCode, dir, filename);
        String type = contentType == null || contentType.isBlank()
                ? "application/octet-stream" : contentType;
        HttpResponse<String> response = send(
                signedRequest("PUT", key, body, type), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw BusinessException.of(ErrorCode.STORAGE_ERROR,
                    "对象存储上传失败，HTTP " + response.statusCode());
        }
        return new StoredFile(key, urlOf(key), body.length);
    }

    @Override
    public Resource download(String tenantCode, String objectKey) {
        assertOwnership(tenantCode, objectKey);
        HttpResponse<byte[]> response = send(
                signedRequest("GET", objectKey, new byte[0], null),
                HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() == 404) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "文件不存在或已被删除");
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw BusinessException.of(ErrorCode.STORAGE_ERROR,
                    "对象存储下载失败，HTTP " + response.statusCode());
        }
        return new ByteArrayResource(response.body());
    }

    @Override
    public String signedUrl(String tenantCode, String objectKey, Duration ttl) {
        assertOwnership(tenantCode, objectKey);
        long expires = Math.max(1, ttl == null ? 600 : ttl.toSeconds());
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        String amzDate = AMZ_DATE.format(now);
        String dateStamp = DATE_STAMP.format(now);
        String scope = dateStamp + "/" + region() + "/s3/aws4_request";
        String host = host();

        TreeMap<String, String> query = new TreeMap<>();
        query.put("X-Amz-Algorithm", ALGORITHM);
        query.put("X-Amz-Credential", accessKey() + "/" + scope);
        query.put("X-Amz-Date", amzDate);
        query.put("X-Amz-Expires", String.valueOf(expires));
        query.put("X-Amz-SignedHeaders", "host");

        StringJoiner canonicalQuery = new StringJoiner("&");
        query.forEach((k, v) -> canonicalQuery.add(encode(k) + "=" + encode(v)));

        String canonicalRequest = "GET\n" + keyPath(objectKey) + "\n"
                + canonicalQuery + "\n"
                + "host:" + host + "\n\n"
                + "host\n"
                + "UNSIGNED-PAYLOAD";
        String signature = sign(canonicalRequest, amzDate, dateStamp, scope);
        return endpoint + keyPath(objectKey) + "?" + canonicalQuery
                + "&X-Amz-Signature=" + signature;
    }

    @Override
    public void delete(String tenantCode, String objectKey) {
        assertOwnership(tenantCode, objectKey);
        HttpResponse<String> response = send(
                signedRequest("DELETE", objectKey, new byte[0], null),
                HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400 && response.statusCode() != 404) {
            throw BusinessException.of(ErrorCode.STORAGE_ERROR,
                    "对象存储删除失败，HTTP " + response.statusCode());
        }
    }

    @Override
    public void testConnection(String tenantCode) {
        StoredFile probe = upload(tenantCode, ".probe", "probe.txt",
                new ByteArrayInputStream("ok".getBytes(StandardCharsets.UTF_8)),
                2, "text/plain");
        delete(tenantCode, probe.objectKey());
    }

    private HttpRequest signedRequest(String method, String objectKey, byte[] body,
                                      String contentType) {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        String amzDate = AMZ_DATE.format(now);
        String dateStamp = DATE_STAMP.format(now);
        String scope = dateStamp + "/" + region() + "/s3/aws4_request";
        String payloadHash = body.length == 0 ? EMPTY_SHA256 : sha256Hex(body);
        String host = host();
        String path = keyPath(objectKey);

        String canonicalHeaders = "host:" + host + "\n"
                + "x-amz-content-sha256:" + payloadHash + "\n"
                + "x-amz-date:" + amzDate + "\n";
        String signedHeaders = "host;x-amz-content-sha256;x-amz-date";
        String canonicalRequest = method + "\n" + path + "\n\n"
                + canonicalHeaders + "\n"
                + signedHeaders + "\n"
                + payloadHash;
        String signature = sign(canonicalRequest, amzDate, dateStamp, scope);

        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(endpoint + path))
                .timeout(Duration.ofSeconds(20))
                .header("x-amz-date", amzDate)
                .header("x-amz-content-sha256", payloadHash)
                .header("Authorization", ALGORITHM + " Credential=" + accessKey() + "/" + scope
                        + ", SignedHeaders=" + signedHeaders + ", Signature=" + signature);
        if (contentType != null && !contentType.isBlank()) {
            builder.header("Content-Type", contentType);
        }
        HttpRequest.BodyPublisher publisher = body.length == 0
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofByteArray(body);
        return builder.method(method, publisher).build();
    }

    private String sign(String canonicalRequest, String amzDate, String dateStamp, String scope) {
        String stringToSign = ALGORITHM + "\n" + amzDate + "\n" + scope + "\n"
                + sha256Hex(canonicalRequest.getBytes(StandardCharsets.UTF_8));
        byte[] key = hmac(("AWS4" + secretKey()).getBytes(StandardCharsets.UTF_8), dateStamp);
        key = hmac(key, region());
        key = hmac(key, "s3");
        key = hmac(key, "aws4_request");
        return hex(hmac(key, stringToSign));
    }

    private <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> handler) {
        try {
            return httpClient.send(request, handler);
        } catch (IOException e) {
            throw BusinessException.of(ErrorCode.STORAGE_ERROR,
                    "对象存储连接失败：" + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw BusinessException.of(ErrorCode.STORAGE_ERROR, "对象存储请求被中断");
        }
    }

    private String buildKey(String tenantCode, String dir, String filename) {
        String cleanDir = dir == null ? "" : dir.replaceAll("^/+|/+$", "");
        String ext = "";
        if (filename != null) {
            int dot = filename.lastIndexOf('.');
            if (dot > -1 && filename.length() - dot <= 10) {
                ext = filename.substring(dot).toLowerCase(Locale.ROOT);
            }
        }
        String name = UUID.randomUUID().toString().replace("-", "") + ext;
        return cleanDir.isEmpty()
                ? "tenants/" + tenantCode + "/" + name
                : "tenants/" + tenantCode + "/" + cleanDir + "/" + name;
    }

    /** path-style 时路径为 /bucket/key，virtual-host 时直接 /key 并把 bucket 拼到域名。 */
    private String keyPath(String objectKey) {
        String key = "/" + objectKey.replaceAll("^/+", "");
        return settings.isPathStyle() ? "/" + bucket + encodePath(key) : encodePath(key);
    }

    private String host() {
        String host = endpoint.replaceFirst("^https?://", "");
        return settings.isPathStyle() ? host : bucket + "." + host;
    }

    private String urlOf(String objectKey) {
        String base = settings.getPublicBaseUrl();
        if (base == null || base.isBlank()) {
            base = endpoint + (settings.isPathStyle() ? "/" + bucket : "");
        }
        return base.replaceAll("/+$", "") + "/" + objectKey;
    }

    private String region() {
        return settings.getRegion() == null || settings.getRegion().isBlank()
                ? "us-east-1" : settings.getRegion();
    }

    private String accessKey() {
        return settings.getAccessKey() == null ? "" : settings.getAccessKey();
    }

    private String secretKey() {
        return settings.getSecretKey() == null ? "" : settings.getSecretKey();
    }

    private void requireTenant(String tenantCode) {
        if (tenantCode == null || tenantCode.isBlank()) {
            throw BusinessException.of(ErrorCode.STORAGE_PATH_DENIED, "缺少租户标识，禁止写入存储");
        }
    }

    private void assertOwnership(String tenantCode, String objectKey) {
        requireTenant(tenantCode);
        if (objectKey == null || !objectKey.startsWith("tenants/" + tenantCode + "/")) {
            throw BusinessException.of(ErrorCode.STORAGE_PATH_DENIED);
        }
    }

    private static String encodePath(String path) {
        StringJoiner joiner = new StringJoiner("/");
        for (String segment : path.split("/", -1)) {
            joiner.add(encode(segment));
        }
        return joiner.toString();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8)
                .replace("+", "%20")
                .replace("*", "%2A")
                .replace("%7E", "~");
    }

    private static String sha256Hex(byte[] data) {
        try {
            return hex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }

    private static byte[] hmac(byte[] key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("HmacSHA256 不可用", e);
        }
    }

    private static String hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }
}
