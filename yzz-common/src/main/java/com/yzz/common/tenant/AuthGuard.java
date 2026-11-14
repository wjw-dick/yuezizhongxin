package com.yzz.common.tenant;

import com.yzz.common.api.BusinessException;
import com.yzz.common.api.ErrorCode;

/**
 * 登录与角色校验工具。平台端与租户端入口物理隔离，越界访问直接拒绝。
 */
public final class AuthGuard {

    private AuthGuard() {
    }

    public static void requireLogin() {
        if (TenantContext.getUserId() == null) {
            throw BusinessException.of(ErrorCode.UNAUTHORIZED);
        }
    }

    public static void requirePlatform() {
        requireLogin();
        if (!TenantContext.isPlatform()) {
            throw BusinessException.of(ErrorCode.END_MISMATCH);
        }
    }

    public static void requirePlatformRole(String... roleCodes) {
        requirePlatform();
        String role = TenantContext.getRoleCode();
        for (String code : roleCodes) {
            if (code.equals(role)) {
                return;
            }
        }
        throw BusinessException.of(ErrorCode.FORBIDDEN);
    }

    public static void requireTenant() {
        requireLogin();
        if (TenantContext.isPlatform()) {
            throw BusinessException.of(ErrorCode.END_MISMATCH);
        }
        if (TenantContext.getTenantId() == null) {
            throw new TenantAccessDeniedException("缺少租户标识，拒绝访问租户业务接口");
        }
    }

    /**
     * 校验资源归属当前租户。任何按主键查询到实体后都必须调用该方法做二次确认，
     * 防止绕过租户条件直接访问他人资源标识。
     */
    public static void requireSameTenant(Long ownerTenantId) {
        Long current = TenantContext.getTenantId();
        if (ownerTenantId == null || current == null || !ownerTenantId.equals(current)) {
            throw new TenantAccessDeniedException("资源不属于当前租户");
        }
    }
}
