package com.yzz.common.web;

import com.yzz.common.api.BusinessException;
import com.yzz.common.api.ErrorCode;
import com.yzz.common.tenant.TenantAccessDeniedException;
import com.yzz.common.tenant.TenantContext;
import com.yzz.common.tenant.TenantState;
import com.yzz.common.tenant.TenantStatusProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 租户上下文过滤器（链路第一环）。
 *
 * <p>解析顺序：请求头 X-Tenant-Code → 子域名。解析后立即校验租户五态：
 * 停用与已到期一律拒绝（含登录接口），欠费允许登录但由拦截器限制写操作。
 * 请求结束无条件清理 ThreadLocal。</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class TenantContextFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(TenantContextFilter.class);
    public static final String HEADER = "X-Tenant-Code";
    private static final java.util.regex.Pattern IP_PATTERN =
            java.util.regex.Pattern.compile("^\\d{1,3}(\\.\\d{1,3}){3}$");

    private final ObjectProvider<TenantStatusProvider> statusProvider;

    public TenantContextFilter(ObjectProvider<TenantStatusProvider> statusProvider) {
        this.statusProvider = statusProvider;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/files/")
                || uri.startsWith("/actuator")
                || uri.startsWith("/v3/api-docs")
                || uri.startsWith("/swagger-ui")
                || uri.equals("/error");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String tenantCode = resolveTenantCode(request);
            if (tenantCode != null) {
                TenantContext.initTenant(null, tenantCode);
                TenantStatusProvider provider = statusProvider.getIfAvailable();
                if (provider != null) {
                    Long tenantId = provider.idOf(tenantCode);
                    if (tenantId != null) {
                        TenantContext.initTenant(tenantId, tenantCode);
                    }
                    TenantState state = provider.stateOf(tenantCode);
                    if (state == TenantState.UNKNOWN) {
                        // 已带令牌却指向不存在的租户，按越权处理（40300 / 403）；
                        // 登录等无令牌请求才按租户不存在返回 40102 / 401。
                        if (request.getHeader("Authorization") != null) {
                            throw new TenantAccessDeniedException("请求头租户标识与登录租户不一致或租户不存在");
                        }
                        throw BusinessException.of(ErrorCode.TENANT_NOT_FOUND);
                    }
                    TenantContext.holder().setTenantState(state.name());
                    if (!state.accessible()) {
                        throw new BusinessException(
                                state == TenantState.DISABLED ? ErrorCode.TENANT_DISABLED : ErrorCode.TENANT_EXPIRED);
                    }
                }
            }
            filterChain.doFilter(request, response);
        } catch (TenantAccessDeniedException e) {
            response.setStatus(403);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":" + ErrorCode.TENANT_ACCESS_DENIED.getCode()
                    + ",\"msg\":\"" + e.getMessage() + "\",\"data\":null}");
        } catch (BusinessException e) {
            // 过滤器内异常无法被 @RestControllerAdvice 捕获，这里显式输出统一结构
            response.setStatus(e.getErrorCode() == ErrorCode.TENANT_NOT_FOUND ? 401 : 403);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":" + e.getErrorCode().getCode()
                    + ",\"msg\":\"" + e.getMessage() + "\",\"data\":null}");
        } finally {
            TenantContext.clear();
        }
    }

    private String resolveTenantCode(HttpServletRequest request) {
        String header = request.getHeader(HEADER);
        if (header != null && !header.isBlank()) {
            return header.trim();
        }
        String host = request.getServerName();
        if (host == null) {
            return null;
        }
        // IP 直连、localhost、IPv6 无法承载子域名，只允许显式请求头传递租户标识
        if (!host.contains(".") || IP_PATTERN.matcher(host).matches() || "localhost".equalsIgnoreCase(host)) {
            log.debug("非域名访问 host={} uri={}", host, request.getRequestURI());
            return null;
        }
        String[] parts = host.split("\\.");
        if (parts.length >= 3 && !"www".equals(parts[0]) && !"api".equals(parts[0])) {
            return parts[0];
        }
        log.debug("未解析到租户标识 host={} uri={}", host, request.getRequestURI());
        return null;
    }
}
