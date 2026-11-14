package com.yzz.platform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 租户开通请求。开通向导四步的入参合并为一次提交：基本信息 → 套餐 → 配额 → 生成主管理员。
 */
@Schema(description = "租户开通请求")
public record TenantCreateRequest(
        @Schema(description = "租户标识，用于子域名与存储路径，小写字母/数字/中划线", example = "yuezi-hz")
        @NotBlank(message = "请填写租户标识")
        String tenantCode,

        @Schema(description = "租户名称", example = "杭州西湖旗舰店")
        @NotBlank(message = "请填写租户名称")
        String tenantName,

        @Schema(description = "所属行业", example = "母婴照护")
        String industry,

        @Schema(description = "联系人")
        String contactName,

        @Schema(description = "联系电话")
        String contactPhone,

        @Schema(description = "套餐标识")
        @NotNull(message = "请选择套餐")
        Long packageId,

        @Schema(description = "试用天数，0 表示直接开通正式服务", example = "14")
        Integer trialDays,

        @Schema(description = "主管理员账号", example = "admin")
        @NotBlank(message = "请填写主管理员账号")
        String adminUsername,

        @Schema(description = "主管理员姓名")
        String adminRealName) {
}
