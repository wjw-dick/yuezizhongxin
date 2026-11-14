package com.yzz.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 租户后台登录请求。租户标识可来自请求体、请求头或子域名。
 */
@Schema(description = "租户后台登录请求")
public record TenantLoginRequest(
        @Schema(description = "租户标识，留空则取请求头 X-Tenant-Code 或子域名", example = "demo")
        String tenantCode,

        @Schema(description = "账号", example = "admin")
        @NotBlank(message = "请输入账号") String username,

        @Schema(description = "密码")
        @NotBlank(message = "请输入密码") String password) {
}
