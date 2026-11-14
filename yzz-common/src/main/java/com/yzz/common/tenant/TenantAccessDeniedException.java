package com.yzz.common.tenant;

/**
 * 跨租户越权异常。固定映射为 HTTP 403 与错误码 40300，并写入安全审计。
 */
public class TenantAccessDeniedException extends RuntimeException {

    public TenantAccessDeniedException(String message) {
        super(message);
    }
}
