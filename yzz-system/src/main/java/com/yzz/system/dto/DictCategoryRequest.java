package com.yzz.system.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 字典分类维护参数。编码仅内部使用，中文名必填。
 */
public record DictCategoryRequest(
        @NotBlank(message = "字典分类编码不能为空") String category,
        @NotBlank(message = "字典分类中文名不能为空") String name,
        Integer sort,
        String status) {
}
