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

import java.time.Duration;

/**
 * 限流切面：按「接口 + 租户 + 登录主体（未登录按 IP）」计数。
 */
@Aspect
@Component
public class RateLimitAspect {

    private final CacheStore cacheStore;

    public RateLimitAspect(CacheStore cacheStore) {
        this.cacheStore = cacheStore;
    }

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        HttpServletRequest request = currentRequest();
        String uri = request == null ? joinPoint.getSignature().toShortString() : request.getRequestURI();
        String subject = TenantContext.getUserId() != null
                ? String.valueOf(TenantContext.getUserId())
                : (request == null ? "anonymous" : request.getRemoteAddr());
        String key = "rate:" + TenantContext.getTenantId() + ":" + uri + ":" + subject;
        long current = cacheStore.increment(key, Duration.ofSeconds(rateLimit.seconds()));
        if (current > rateLimit.count()) {
            throw BusinessException.of(ErrorCode.RATE_LIMITED);
        }
        return joinPoint.proceed();
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes == null ? null : attributes.getRequest();
    }
}
