package com.yzz.platform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Schema(description = "套餐保存请求")
public record PackageSaveRequest(
        @Schema(description = "套餐标识，新增时为空") Long id,

        @Schema(description = "套餐名称")
        @NotBlank(message = "请填写套餐名称")
        String packageName,

        @Schema(description = "套餐价格（元）")
        @NotNull(message = "请填写套餐价格")
        BigDecimal price,

        @Schema(description = "计费周期：月/年")
        @NotBlank(message = "请选择计费周期")
        String billingCycle,

        @Schema(description = "含端数量") Integer endCount,
        @Schema(description = "席位额度") @NotNull(message = "请填写席位额度") Integer seatQuota,
        @Schema(description = "公众号额度") Integer mpQuota,
        @Schema(description = "存储额度（GB）") Integer storageQuotaGb,
        @Schema(description = "短信额度（条）") Integer smsQuota,
        @Schema(description = "接口额度") Integer apiQuota,
        @Schema(description = "是否支持试用：1 是，0 否") Integer trialSupported,
        @Schema(description = "备注") String remark) {
}
