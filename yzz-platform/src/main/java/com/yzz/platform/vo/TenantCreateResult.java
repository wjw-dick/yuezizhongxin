package com.yzz.platform.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 租户开通结果。初始口令仅此一次返回，之后不可再查。
 */
@Schema(description = "租户开通结果")
public record TenantCreateResult(
        Long tenantId,
        String tenantCode,
        String adminUsername,
        String initialPassword) {
}
