package com.yzz.mp.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yzz.common.guard.CacheStore;
import com.yzz.mp.entity.MpAccount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 微信公众号接口基础层（T63）。
 *
 * <p>只负责：access_token fetch/cache、通用 GET/POST 调用、错误码原样返回。
 * 频控与配额由调用方按错误码 45009/45011 提示，不在此层重试，避免放大限流。
 * 未配置 AppID/AppSecret 时返回 {@code configured=false}，由业务层降级为本地数据。</p>
 */
@Component
public class WeChatMpClient {

    private static final Logger log = LoggerFactory.getLogger(WeChatMpClient.class);
    private static final String HOST = "https://api.weixin.qq.com";
    private static final String TOKEN_KEY = "mp:access_token:";

    private final CacheStore cacheStore;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();

    public WeChatMpClient(CacheStore cacheStore, ObjectMapper objectMapper) {
        this.cacheStore = cacheStore;
        this.objectMapper = objectMapper;
    }

    public boolean configured(MpAccount account) {
        return account != null && account.getAppId() != null && !account.getAppId().isBlank()
                && account.getAppSecret() != null && !account.getAppSecret().isBlank();
    }

    /**
     * 获取并缓存 access_token（微信有效期 7200 秒，这里缓存 7000 秒）。
     */
    public String accessToken(MpAccount account) {
        String cached = cacheStore.get(TOKEN_KEY + account.getAppId());
        if (cached != null) {
            return cached;
        }
        String url = HOST + "/cgi-bin/token?grant_type=client_credential&appid=" + account.getAppId()
                + "&secret=" + account.getAppSecret();
        JsonNode node = get(url);
        String token = node.path("access_token").asText("");
        if (token.isBlank()) {
            throw new MpApiException(node.path("errcode").asInt(-1),
                    node.path("errmsg").asText("获取 access_token 失败"));
        }
        cacheStore.put(TOKEN_KEY + account.getAppId(), token, Duration.ofSeconds(7000));
        return token;
    }

    public JsonNode get(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(10)).GET().build();
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return objectMapper.readTree(response.body());
        } catch (Exception e) {
            throw new MpApiException(-1, "微信接口调用失败：" + e.getMessage());
        }
    }

    public JsonNode post(String url, Object body) {
        try {
            String json = body == null ? "{}" : objectMapper.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8)).build();
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return objectMapper.readTree(response.body());
        } catch (Exception e) {
            throw new MpApiException(-1, "微信接口调用失败：" + e.getMessage());
        }
    }

    /** 带 token 的 GET。 */
    public JsonNode apiGet(MpAccount account, String path) {
        return get(HOST + path + (path.contains("?") ? "&" : "?") + "access_token=" + accessToken(account));
    }

    /** 带 token 的 POST。 */
    public JsonNode apiPost(MpAccount account, String path, Object body) {
        return post(HOST + path + "?access_token=" + accessToken(account), body);
    }

    /** 服务器地址校验签名：sha1(sort(token, timestamp, nonce))。 */
    public static boolean verifySignature(String token, String timestamp, String nonce, String signature) {
        if (token == null || timestamp == null || nonce == null || signature == null) {
            return false;
        }
        String[] arr = {token, timestamp, nonce};
        java.util.Arrays.sort(arr);
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            byte[] digest = sha1.digest(String.join("", arr).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString().equalsIgnoreCase(signature);
        } catch (Exception e) {
            return false;
        }
    }

    public Map<String, Object> errorOf(JsonNode node) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("errcode", node.path("errcode").asInt(0));
        error.put("errmsg", node.path("errmsg").asText(""));
        return error;
    }

    /** 微信接口异常，errcode 原样保留便于对照官方文档排查。 */
    public static class MpApiException extends RuntimeException {

        private final int errcode;

        public MpApiException(int errcode, String message) {
            super(message);
            this.errcode = errcode;
        }

        public int getErrcode() {
            return errcode;
        }
    }
}
