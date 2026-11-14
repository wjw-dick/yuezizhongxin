package com.yzz.system.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * SSO 配置保存参数。只填服务器根地址，协议路径由服务端拼接。
 */
public record SsoConfigRequest(
        boolean enabled,
        @NotBlank(message = "请选择 SSO 协议") String protocol,
        @NotBlank(message = "服务器地址不能为空") String serverUrl,
        String serviceUrl,
        String clientId,
        String clientSecret,
        String accountMapping,
        Boolean fallbackEnabled,
        String rejectStrategy) {
}
