package com.yzz.platform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 平台端登录请求。平台账号与租户账号不互通，走独立登录入口。
 */
@Schema(description = "平台端登录请求")
public record PlatformLoginRequest(
        @Schema(description = "平台账号", example = "platform")
        @NotBlank(message = "请输入平台账号") String username,

        @Schema(description = "登录密码", example = "Platform@123")
        @NotBlank(message = "请输入登录密码") String password) {
}
