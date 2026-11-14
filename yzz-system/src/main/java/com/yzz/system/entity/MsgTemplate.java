package com.yzz.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息模板：同节点同通道唯一，字段映射形如 thing1=客户姓名,time2=入住时间。
 */
@Data
@TableName("msg_template")
public class MsgTemplate {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String triggerNode;

    /** 公众号模板消息 / 小程序订阅消息 / App 推送 / 站内信 / 短信。 */
    private String channel;

    private String templateCode;

    private String fieldMapping;

    private LocalDateTime createdTime;
}
