package com.yzz.common.api;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 登录结果。平台端与租户端共用同一结构，token 内的 platform 标记决定可访问的接口范围。
 */
@Schema(description = "登录结果")
public record LoginVO(
        @Schema(description = "登录令牌") String token,
        @Schema(description = "账号") String username,
        @Schema(description = "姓名") String realName,
        @Schema(description = "角色编码") String roleCode,
        @Schema(description = "角色中文名") String roleName,
        @Schema(description = "是否平台账号") boolean platform,
        @Schema(description = "租户编码，平台账号为空") String tenantCode) {
}
