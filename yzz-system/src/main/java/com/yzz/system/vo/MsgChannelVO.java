package com.yzz.system.vo;

/**
 * 消息通道回显。渠道参数中的密钥已掩码。
 */
public record MsgChannelVO(String channelCode, String channelName, boolean enabled,
                           String configJson, boolean configured) {
}
