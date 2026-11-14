package com.yzz.mp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 公众号消息流水。错误码原样落库。
 */
@Data
@TableName("mp_message")
public class MpMessage {

    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户标识。 */
    private Long tenantId;

    /** 公众号标识。 */
    private Long accountId;

    /** 用户标识。 */
    private String openId;

    /** 消息类型。 */
    private String msgType;

    /** 发送方。 */
    private String sender;

    /** 内容。 */
    private String content;

    /** 原始报文。 */
    private String rawPayload;

    /** 错误码。 */
    private String errorCode;

    /** 错误描述。 */
    private String errorMsg;

    /** 创建时间。 */
    private LocalDateTime createdTime;
}
