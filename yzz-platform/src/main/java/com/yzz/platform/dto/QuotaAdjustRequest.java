package com.yzz.platform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 单租户配额调整。必须填写原因，写审计留痕。
 */
@Schema(description = "配额调整请求")
public record QuotaAdjustRequest(
        @Schema(description = "席位上限") Integer seatLimit,
        @Schema(description = "公众号数上限") Integer mpLimit,
        @Schema(description = "存储容量上限（GB）") Integer storageLimitGb,
        @Schema(description = "短信条数上限") Integer smsLimit,
        @Schema(description = "接口调用量上限") Integer apiLimit,
        @Schema(description = "调整原因")
        @NotBlank(message = "请填写配额调整原因")
        String reason) {
}
