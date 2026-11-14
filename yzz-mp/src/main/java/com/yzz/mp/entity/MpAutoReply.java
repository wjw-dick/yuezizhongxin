package com.yzz.mp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 自动回复。
 */
@Data
@TableName("mp_auto_reply")
public class MpAutoReply {

    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户标识。 */
    private Long tenantId;

    /** 公众号标识。 */
    private Long accountId;

    /** 类型。 */
    private String replyType;

    /** 关键词。 */
    private String keyword;

    /** 匹配类型。 */
    private String matchType;

    /** 回复消息类型。 */
    private String msgType;

    /** 回复内容。 */
    private String content;

    /** 创建时间。 */
    private LocalDateTime createdTime;
}
