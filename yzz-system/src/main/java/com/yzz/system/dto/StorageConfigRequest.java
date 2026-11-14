package com.yzz.system.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 对象存储配置保存参数。密钥为空表示沿用已保存值；confirmSwitch 为切换驱动的二次确认标记。
 */
public record StorageConfigRequest(
        @NotBlank(message = "请选择存储驱动") String driver,
        String endpoint,
        String region,
        String bucket,
        String accessKey,
        String secretKey,
        Boolean pathStyle,
        String publicBaseUrl,
        boolean confirmSwitch) {
}
