package com.yzz.common.web;

import com.yzz.common.api.BusinessException;
import com.yzz.common.api.ErrorCode;
import com.yzz.common.audit.AuditRecorder;
import com.yzz.common.tenant.TenantAccessDeniedException;
import com.yzz.common.tenant.TenantContext;
import com.yzz.common.util.TokenUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 认证与入口隔离拦截器。
 *
 * <p>职责三件事：解析令牌写入上下文、校验平台端/租户端入口互不越界、
 * 校验前端传入的租户标识与令牌内租户一致（篡改租户标识即 403 并写安全审计）。</p>
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(AuthInterceptor.class);
    private static final String BEARER = "Bearer ";

    private final TokenUtil tokenUtil;
    private final ObjectProvider<AuditRecorder> auditRecorder;

    public AuthInterceptor(TokenUtil tokenUtil, ObjectProvider<AuditRecorder> auditRecorder) {
        this.tokenUtil = tokenUtil;
        this.auditRecorder = auditRecorder;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith(BEARER)) {
            throw BusinessException.of(ErrorCode.UNAUTHORIZED);
        }
        TokenUtil.Claims claims = tokenUtil.parse(authorization.substring(BEARER.length()).trim());

        boolean platformPath = request.getRequestURI().startsWith("/api/platform");
        if (claims.platform() != platformPath) {
            writeSecurityAudit(request, "入口越界", "platform=" + claims.platform() + " uri=" + request.getRequestURI());
            throw BusinessException.of(ErrorCode.END_MISMATCH);
        }

        String headerTenant = request.getHeader(TenantContextFilter.HEADER);
        if (!claims.platform() && headerTenant != null && !headerTenant.isBlank()
                && !headerTenant.trim().equals(claims.tenantCode())) {
            writeSecurityAudit(request, "篡改租户标识",
                    "token=" + claims.tenantCode() + " header=" + headerTenant);
            throw new TenantAccessDeniedException("请求头租户标识与登录租户不一致");
        }

        TenantContext.initLogin(claims.userId(), null, claims.platform(), claims.roleCode());
        if (claims.tenantId() != null) {
            TenantContext.initTenant(claims.tenantId(), claims.tenantCode());
        }
        return true;
    }

    private void writeSecurityAudit(HttpServletRequest request, String action, String detail) {
        AuditRecorder recorder = auditRecorder.getIfAvailable();
        if (recorder != null) {
            try {
                recorder.recordSecurity(action, detail, clientIp(request));
            } catch (Exception e) {
                log.error("安全审计写入失败 action={}", action, e);
            }
        }
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded != null && !forwarded.isBlank()
                ? forwarded.split(",")[0].trim()
                : request.getRemoteAddr();
    }
}
