package com.yzz.system.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * 整树保存菜单节点（T22）。节点以扁平列表提交，parentId 表达层级，sort 表达同级顺序。
 */
public record NavTreeRequest(
        @NotBlank(message = "请选择适用端") String endType,
        @NotEmpty(message = "菜单不能为空") @Valid List<Node> nodes) {

    public record Node(
            Long id,
            Long parentId,
            @NotBlank(message = "菜单标题不能为空") String title,
            String linkType,
            String path,
            String icon,
            Integer sort,
            Integer visible) {
    }
}
