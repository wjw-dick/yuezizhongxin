package com.yzz.system.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 连接测试参数：未保存的临时配置也可以先测通再保存。
 */
public record StorageTestRequest(
        @NotBlank(message = "请选择存储驱动") String driver,
        String endpoint,
        String region,
        String bucket,
        String accessKey,
        String secretKey,
        Boolean pathStyle) {
}
