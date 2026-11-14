package com.yzz.common.util;

import com.yzz.common.api.BusinessException;
import com.yzz.common.api.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

/**
 * 登录令牌签发与校验（HMAC-SHA256 签名，载荷含租户标识，防篡改）。
 *
 * <p>载荷格式：userId|tenantId|tenantCode|platform|roleCode|expireEpochSecond。
 * 租户标识随令牌下发，服务端不接受前端单独传入的租户标识作为授权依据。</p>
 */
@Component
public class TokenUtil {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String SPLIT = "|";

    private final SecretKeySpec keySpec;
    private final long ttlSeconds;

    public TokenUtil(@Value("${yzz.security.token-secret}") String secret,
                     @Value("${yzz.security.token-ttl-minutes:720}") long ttlMinutes) {
        this.keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
        this.ttlSeconds = ttlMinutes * 60;
    }

    public String issue(Long userId, Long tenantId, String tenantCode, boolean platform, String roleCode) {
        long expire = Instant.now().getEpochSecond() + ttlSeconds;
        String payload = String.join(SPLIT,
                String.valueOf(userId),
                tenantId == null ? "" : String.valueOf(tenantId),
                tenantCode == null ? "" : tenantCode,
                platform ? "1" : "0",
                roleCode == null ? "" : roleCode,
                String.valueOf(expire));
        String encoded = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return encoded + "." + sign(encoded);
    }

    public Claims parse(String token) {
        if (token == null || token.isBlank() || !token.contains(".")) {
            throw BusinessException.of(ErrorCode.UNAUTHORIZED);
        }
        int idx = token.lastIndexOf('.');
        String encoded = token.substring(0, idx);
        String signature = token.substring(idx + 1);
        if (!MessageDigest.isEqual(sign(encoded).getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8))) {
            throw BusinessException.of(ErrorCode.UNAUTHORIZED, "登录令牌签名校验失败");
        }
        String payload = new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
        String[] parts = payload.split("\\" + SPLIT, -1);
        if (parts.length != 6) {
            throw BusinessException.of(ErrorCode.UNAUTHORIZED, "登录令牌格式不合法");
        }
        long expire = Long.parseLong(parts[5]);
        if (Instant.now().getEpochSecond() > expire) {
            throw BusinessException.of(ErrorCode.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        return new Claims(
                Long.parseLong(parts[0]),
                parts[1].isBlank() ? null : Long.parseLong(parts[1]),
                parts[2].isBlank() ? null : parts[2],
                "1".equals(parts[3]),
                parts[4].isBlank() ? null : parts[4]);
    }

    private String sign(String encoded) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(keySpec);
            byte[] raw = mac.doFinal(encoded.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        } catch (Exception e) {
            throw BusinessException.of(ErrorCode.INTERNAL_ERROR, "登录令牌签发失败");
        }
    }

    /** 令牌载荷。 */
    public record Claims(Long userId, Long tenantId, String tenantCode, boolean platform, String roleCode) {
    }
}
