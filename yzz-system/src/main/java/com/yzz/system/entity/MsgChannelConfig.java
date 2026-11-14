package com.yzz.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息通道配置：4 个消息通道 + 6 个短信渠道，短信渠道全局互斥仅启用一条。
 */
@Data
@TableName("msg_channel_config")
public class MsgChannelConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    /** 短信渠道编码：aliyun / tencent / huawei / jd / aws_sns / local_log。 */
    private String channelCode;

    private String channelName;

    /** 1 启用，0 停用；同一时刻仅一条短信渠道为 1。 */
    private Integer enabled;

    /** 渠道参数 JSON，密钥加密。 */
    private String configJson;

    private LocalDateTime updatedTime;
}
