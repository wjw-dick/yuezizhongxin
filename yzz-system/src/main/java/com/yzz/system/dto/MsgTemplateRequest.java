package com.yzz.system.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 消息模板保存参数。同节点同通道唯一。
 */
public record MsgTemplateRequest(
        @NotBlank(message = "请选择触发节点") String triggerNode,
        @NotBlank(message = "请选择通道") String channel,
        @NotBlank(message = "模板编号不能为空") String templateCode,
        String fieldMapping) {
}
