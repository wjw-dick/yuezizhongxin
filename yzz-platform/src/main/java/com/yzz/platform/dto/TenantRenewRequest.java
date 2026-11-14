package com.yzz.platform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "租户续期请求")
public record TenantRenewRequest(
        @Schema(description = "续期天数", example = "365")
        @NotNull(message = "请填写续期天数")
        @Min(value = 1, message = "续期天数至少 1 天")
        Integer days) {
}
