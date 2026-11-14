package com.yzz.system.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 语言维护参数。
 */
public record LanguageRequest(
        @NotBlank(message = "语言编码不能为空") String code,
        @NotBlank(message = "语言中文名不能为空") String name,
        Boolean enabled,
        Boolean isDefault,
        Integer sort) {
}
