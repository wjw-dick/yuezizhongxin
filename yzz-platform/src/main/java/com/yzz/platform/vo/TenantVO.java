package com.yzz.platform.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 租户列表/详情返回。平台端只能看到汇总与状态，不返回任何业务明细。
 */
@Schema(description = "租户汇总信息")
public record TenantVO(
        Long id,
        String tenantCode,
        String tenantName,
        String industry,
        String contactName,
        String contactPhone,
        Long packageId,
        String packageName,
        String status,
        String statusName,
        LocalDateTime trialEndTime,
        LocalDateTime expireTime,
        Integer seatLimit,
        Integer seatUsed,
        Integer mpLimit,
        Integer mpUsed,
        Integer storageLimitGb,
        Integer storageUsedGb,
        Integer smsLimit,
        Integer smsUsed,
        Integer apiLimit,
        Integer apiUsed,
        LocalDateTime createdTime) {
}
