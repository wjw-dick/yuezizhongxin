package com.yzz.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * 语言包批量保存：一次提交某个语言的多个文案键。
 */
public record I18nMessageRequest(
        @NotBlank(message = "请指定语言") String langCode,
        @NotNull(message = "文案内容不能为空") Map<String, String> messages) {
}
