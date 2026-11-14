package com.yzz.system.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 字典项维护参数。
 */
public record DictItemRequest(
        @NotBlank(message = "字典项编码不能为空") String code,
        @NotBlank(message = "字典项中文名称不能为空") String name,
        String color,
        Integer sort,
        String status) {
}
