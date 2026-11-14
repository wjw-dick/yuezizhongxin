package com.yzz.system.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * 按分组保存系统参数。
 *
 * <p>金额与比例类参数变更属于敏感操作：首次提交返回待确认清单（错误码 40900），
 * 前端弹窗确认后带 {@code confirm=true} 再次提交才落库并写审计。</p>
 */
public record ConfigSaveRequest(
        @NotBlank(message = "请指定参数分组") String group,
        @NotEmpty(message = "提交内容不能为空") @Valid List<Item> items,
        boolean confirm) {

    public record Item(
            @NotBlank(message = "参数键不能为空") String key,
            String value,
            String valueType,
            String remark) {
    }
}
