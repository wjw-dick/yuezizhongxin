package com.yzz.system.vo;

import java.time.LocalDateTime;

/**
 * 对象存储配置回显：密钥只回掩码，避免明文出接口。
 */
public record StorageConfigVO(
        String driver,
        String endpoint,
        String region,
        String bucket,
        String accessKeyMasked,
        String secretKeyMasked,
        boolean pathStyle,
        String publicBaseUrl,
        boolean tested,
        LocalDateTime updatedTime) {
}
