package com.yzz.mp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 图文草稿与发表记录。
 */
@Data
@TableName("mp_draft")
public class MpDraft {

    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户标识。 */
    private Long tenantId;

    /** 公众号标识。 */
    private Long accountId;

    /** 标题。 */
    private String title;

    /** 作者。 */
    private String author;

    /** 原文地址。 */
    private String sourceUrl;

    /** 封面图键。 */
    private String coverKey;

    /** 摘要。 */
    private String digest;

    /** 富文本正文。 */
    private String bodyHtml;

    /** 多图文序号。 */
    private Integer articleIndex;

    /** 草稿标识。 */
    private String draftMediaId;

    /** 状态。 */
    private String status;

    /** 发表时间。 */
    private LocalDateTime publishTime;

    /** 发表结果。 */
    private String publishResult;

    /** 失败原因。 */
    private String failReason;

    /** 创建时间。 */
    private LocalDateTime createdTime;
}
