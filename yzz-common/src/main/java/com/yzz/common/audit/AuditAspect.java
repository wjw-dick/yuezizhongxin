package com.yzz.common.audit;

import com.yzz.common.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * 审计切面：对标注 {@link AuditLog} 的方法记录成功/失败结果与操作人。
 *
 * <p>审计写入失败不影响主链路，只打印错误日志，避免审计组件故障拖垮业务。</p>
 */
@Aspect
@Component
public class AuditAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);
    private static final int MAX_DETAIL_LENGTH = 500;

    private final ObjectProvider<AuditRecorder> recorderProvider;

    public AuditAspect(ObjectProvider<AuditRecorder> recorderProvider) {
        this.recorderProvider = recorderProvider;
    }

    @Around("@annotation(auditLog)")
    public Object around(ProceedingJoinPoint joinPoint, AuditLog auditLog) throws Throwable {
        boolean success = true;
        String error = null;
        Object result = null;
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable t) {
            success = false;
            error = t.getMessage();
            throw t;
        } finally {
            try {
                AuditRecorder recorder = recorderProvider.getIfAvailable();
                if (recorder != null) {
                    recorder.record(buildEvent(joinPoint, auditLog, success, error));
                }
            } catch (Exception e) {
                log.error("审计日志写入失败 module={} action={}", auditLog.module(), auditLog.action(), e);
            }
        }
    }

    private AuditEvent buildEvent(ProceedingJoinPoint joinPoint, AuditLog auditLog,
                                  boolean success, String error) {
        TenantContext.Holder holder = TenantContext.holder();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String target = signature.getDeclaringType().getSimpleName() + "#" + signature.getName();
        String detail = auditLog.withArgs()
                ? truncate(Arrays.toString(joinPoint.getArgs()))
                : "参数已省略";
        return new AuditEvent(
                auditLog.module(),
                auditLog.action(),
                target,
                detail,
                success,
                error,
                holder.getTenantId(),
                holder.getTenantCode(),
                holder.isPlatform(),
                holder.getUserId(),
                holder.getUsername(),
                clientIp(),
                LocalDateTime.now());
    }

    private String clientIp() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "";
        }
        HttpServletRequest request = attributes.getRequest();
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String truncate(String text) {
        if (text == null) {
            return "";
        }
        return text.length() <= MAX_DETAIL_LENGTH ? text : text.substring(0, MAX_DETAIL_LENGTH) + "...";
    }
}
