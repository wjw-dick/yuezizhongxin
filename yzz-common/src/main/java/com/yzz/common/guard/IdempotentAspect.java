package com.yzz.common.guard;

import com.yzz.common.api.BusinessException;
import com.yzz.common.api.ErrorCode;
import com.yzz.common.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Arrays;
import java.util.HexFormat;

/**
 * 幂等切面：同一幂等键在窗口期内只放行一次。
 *
 * <p>幂等键优先取请求头 X-Idempotent-Key，其次按「路径 + 租户 + 登录主体 + 入参摘要」生成。</p>
 */
@Aspect
@Component
public class IdempotentAspect {

    private static final String HEADER = "X-Idempotent-Key";

    private final CacheStore cacheStore;

    public IdempotentAspect(CacheStore cacheStore) {
        this.cacheStore = cacheStore;
    }

    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        String key = buildKey(joinPoint, idempotent);
        boolean first = cacheStore.setIfAbsent(key, Duration.ofSeconds(idempotent.ttlSeconds()));
        if (!first) {
            throw BusinessException.of(ErrorCode.DUPLICATE_SUBMIT);
        }
        return joinPoint.proceed();
    }

    private String buildKey(ProceedingJoinPoint joinPoint, Idempotent idempotent) {
        HttpServletRequest request = currentRequest();
        String headerKey = request == null ? null : request.getHeader(HEADER);
        String businessKey;
        if (headerKey != null && !headerKey.isBlank()) {
            businessKey = headerKey;
        } else {
            String path = request == null ? joinPoint.getSignature().toShortString() : request.getRequestURI();
            String raw = path + "|" + TenantContext.getTenantId() + "|" + TenantContext.getUserId()
                    + "|" + Arrays.deepToString(joinPoint.getArgs());
            businessKey = digest(raw);
        }
        String prefix = idempotent.key().isBlank()
                ? "idem:" + TenantContext.getTenantId() + ":"
                : "idem:" + TenantContext.getTenantId() + ":" + idempotent.key() + ":";
        return prefix + businessKey;
    }

    private String digest(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(raw.getBytes(StandardCharsets.UTF_8))).substring(0, 32);
        } catch (Exception e) {
            return String.valueOf(raw.hashCode());
        }
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes == null ? null : attributes.getRequest();
    }
}
