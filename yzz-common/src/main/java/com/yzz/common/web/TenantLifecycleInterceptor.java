package com.yzz.common.web;

import com.yzz.common.api.BusinessException;
import com.yzz.common.api.ErrorCode;
import com.yzz.common.tenant.TenantContext;
import com.yzz.common.tenant.TenantState;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 租户生命周期拦截器：欠费状态下限制新增写操作，停用与到期做二次兜底校验。
 */
@Component
public class TenantLifecycleInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (TenantContext.isPlatform()) {
            return true;
        }
        String stateCode = TenantContext.holder().getTenantState();
        TenantState state = TenantState.of(stateCode);
        if (state == TenantState.DISABLED) {
            throw BusinessException.of(ErrorCode.TENANT_DISABLED);
        }
        if (state == TenantState.EXPIRED) {
            throw BusinessException.of(ErrorCode.TENANT_EXPIRED);
        }
        if (state.writeLimited() && !HttpMethod.GET.matches(request.getMethod())) {
            throw BusinessException.of(ErrorCode.TENANT_ARREARS);
        }
        return true;
    }
}
