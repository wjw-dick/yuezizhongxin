package com.yzz.system.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 消息通道启用/停用与渠道参数保存。
 */
public record MsgChannelRequest(
        @NotBlank(message = "请选择渠道") String channelCode,
        boolean enabled,
        String configJson) {
}
