package com.yzz.order.pay;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 微信支付 v3 客户端（T37）。
 *
 * <p>只做四件事：JSAPI 下单、主动查单、回调验签、回调解密。签名算法为
 * SHA256-RSA2048，报文使用 AES-256-GCM 解密。
 *
 * <p><b>未配置商户参数时进入本地模拟模式</b>：下单返回模拟预支付标识，
 * 后台可用「模拟支付成功」推进订单，便于联调与验收；生产环境必须配置
 * `yzz.pay.enabled=true` 与商户证书，否则不会真正发起扣款。</p>
 */
@Component
public class WeChatPayClient {

    private static final Logger log = LoggerFactory.getLogger(WeChatPayClient.class);
    private static final String HOST = "https://api.mch.weixin.qq.com";

    private final boolean enabled;
    private final String mchId;
    private final String appId;
    private final String serialNo;
    private final String privateKeyPem;
    private final String apiV3Key;
    private final String notifyUrl;
    private final String platformCertPem;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();
    private final SecureRandom random = new SecureRandom();

    public WeChatPayClient(@Value("${yzz.pay.enabled:false}") boolean enabled,
                           @Value("${yzz.pay.mch-id:}") String mchId,
                           @Value("${yzz.pay.app-id:}") String appId,
                           @Value("${yzz.pay.serial-no:}") String serialNo,
                           @Value("${yzz.pay.private-key:}") String privateKeyPem,
                           @Value("${yzz.pay.api-v3-key:}") String apiV3Key,
                           @Value("${yzz.pay.notify-url:}") String notifyUrl,
                           @Value("${yzz.pay.platform-cert:}") String platformCertPem,
                           ObjectMapper objectMapper) {
        this.enabled = enabled;
        this.mchId = mchId;
        this.appId = appId;
        this.serialNo = serialNo;
        this.privateKeyPem = privateKeyPem;
        this.apiV3Key = apiV3Key;
        this.notifyUrl = notifyUrl;
        this.platformCertPem = platformCertPem;
        this.objectMapper = objectMapper;
    }

    public boolean realMode() {
        return enabled && !privateKeyPem.isBlank() && !mchId.isBlank();
    }

    /**
     * JSAPI 下单。
     *
     * @param amountFen 金额（分）
     * @return 小程序/公众号拉起支付所需参数；模拟模式返回 simulated=true
     */
    public Map<String, Object> prepay(String orderNo, String description, long amountFen, String openId) {
        if (!realMode()) {
            Map<String, Object> mock = new LinkedHashMap<>();
            mock.put("simulated", true);
            mock.put("prepayId", "sim_" + orderNo);
            mock.put("orderNo", orderNo);
            mock.put("amountFen", amountFen);
            mock.put("message", "未配置微信支付商户参数，当前为本地模拟模式");
            return mock;
        }
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("appid", appId);
            body.put("mchid", mchId);
            body.put("description", description);
            body.put("out_trade_no", orderNo);
            body.put("notify_url", notifyUrl);
            body.put("amount", Map.of("total", amountFen, "currency", "CNY"));
            if (openId != null && !openId.isBlank()) {
                body.put("payer", Map.of("openid", openId));
            }
            String json = objectMapper.writeValueAsString(body);
            JsonNode node = request("POST", "/v3/pay/transactions/jsapi", json);
            String prepayId = node.path("prepay_id").asText();
            if (prepayId.isBlank()) {
                throw new IllegalStateException("微信下单未返回 prepay_id：" + node);
            }
            return signForMiniProgram(prepayId);
        } catch (Exception e) {
            log.error("微信支付下单失败 orderNo={}", orderNo, e);
            throw new IllegalStateException("微信支付下单失败：" + e.getMessage(), e);
        }
    }

    /** 主动查单。返回 SUCCESS/NOTPAY/USERPAYING/CLOSED/REFUND 等原始状态。 */
    public String query(String orderNo) {
        if (!realMode()) {
            return "SIMULATED";
        }
        try {
            JsonNode node = request("GET", "/v3/pay/transactions/out-trade-no/" + orderNo
                    + "?mchid=" + mchId, null);
            return node.path("trade_state").asText("UNKNOWN");
        } catch (Exception e) {
            log.warn("微信查单失败 orderNo={} err={}", orderNo, e.getMessage());
            return "UNKNOWN";
        }
    }

    /**
     * 回调验签。未配置平台证书时仅允许模拟模式通过。
     */
    public boolean verifyNotify(Map<String, String> headers, String body) {
        if (!realMode()) {
            return true;
        }
        if (platformCertPem == null || platformCertPem.isBlank()) {
            log.error("未配置微信支付平台证书，拒绝回调");
            return false;
        }
        try {
            String timestamp = headers.get("wechatpay-timestamp");
            String nonce = headers.get("wechatpay-nonce");
            String signature = headers.get("wechatpay-signature");
            if (timestamp == null || nonce == null || signature == null) {
                return false;
            }
            if (Math.abs(Instant.now().getEpochSecond() - Long.parseLong(timestamp)) > 300) {
                log.warn("微信回调时间戳超出 5 分钟，按重放拒绝");
                return false;
            }
            String message = timestamp + "\n" + nonce + "\n" + body + "\n";
            Signature verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(loadPublicKey());
            verifier.update(message.getBytes(StandardCharsets.UTF_8));
            return verifier.verify(Base64.getDecoder().decode(signature));
        } catch (Exception e) {
            log.warn("微信回调验签异常", e);
            return false;
        }
    }

    /**
     * 解密回调 resource 字段。
     */
    public JsonNode decryptResource(JsonNode resource) {
        try {
            String ciphertext = resource.path("ciphertext").asText();
            String nonce = resource.path("nonce").asText();
            String associated = resource.path("associated_data").asText("");
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE,
                    new SecretKeySpec(apiV3Key.getBytes(StandardCharsets.UTF_8), "AES"),
                    new GCMParameterSpec(128, nonce.getBytes(StandardCharsets.UTF_8)));
            cipher.updateAAD(associated.getBytes(StandardCharsets.UTF_8));
            byte[] plain = cipher.doFinal(Base64.getDecoder().decode(ciphertext));
            return objectMapper.readTree(plain);
        } catch (Exception e) {
            throw new IllegalStateException("微信回调报文解密失败", e);
        }
    }

    private Map<String, Object> signForMiniProgram(String prepayId) throws Exception {
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String nonce = randomNonce();
        String pkg = "prepay_id=" + prepayId;
        String message = appId + "\n" + timestamp + "\n" + nonce + "\n" + pkg + "\n";
        String signature = sign(message);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("simulated", false);
        result.put("prepayId", prepayId);
        result.put("appId", appId);
        result.put("timeStamp", timestamp);
        result.put("nonceStr", nonce);
        result.put("package", pkg);
        result.put("signType", "RSA");
        result.put("paySign", signature);
        return result;
    }

    private JsonNode request(String method, String path, String body) throws Exception {
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String nonce = randomNonce();
        String message = method + "\n" + path + "\n" + timestamp + "\n" + nonce + "\n"
                + (body == null ? "" : body) + "\n";
        String signature = sign(message);
        String authorization = "WECHATPAY2-SHA256-RSA2048 "
                + "mchid=\"" + mchId + "\","
                + "nonce_str=\"" + nonce + "\","
                + "signature=\"" + signature + "\","
                + "timestamp=\"" + timestamp + "\","
                + "serial_no=\"" + serialNo + "\"";
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(HOST + path))
                .timeout(Duration.ofSeconds(10))
                .header("Authorization", authorization)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json");
        builder = "POST".equals(method)
                ? builder.POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                : builder.GET();
        HttpResponse<String> response = httpClient.send(builder.build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() >= 300) {
            throw new IllegalStateException("微信接口返回 " + response.statusCode() + "：" + response.body());
        }
        return objectMapper.readTree(response.body());
    }

    private String sign(String message) throws Exception {
        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(loadPrivateKey());
        signer.update(message.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signer.sign());
    }

    private PrivateKey loadPrivateKey() throws Exception {
        String pem = privateKeyPem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] der = Base64.getDecoder().decode(pem);
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
    }

    private PublicKey loadPublicKey() throws Exception {
        String pem = platformCertPem
                .replace("-----BEGIN CERTIFICATE-----", "")
                .replace("-----END CERTIFICATE-----", "")
                .replaceAll("\\s", "");
        byte[] der = Base64.getDecoder().decode(pem);
        X509Certificate cert = (X509Certificate) CertificateFactory.getInstance("X.509")
                .generateCertificate(new java.io.ByteArrayInputStream(der));
        return cert.getPublicKey();
    }

    private String randomNonce() {
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        return java.util.HexFormat.of().formatHex(bytes).toUpperCase();
    }
}
