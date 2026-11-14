package com.yzz.platform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "账单生成请求")
public record BillGenerateRequest(
        @Schema(description = "账期，格式 yyyy-MM", example = "2026-11")
        @NotBlank(message = "请填写账期")
        String billPeriod) {
}
