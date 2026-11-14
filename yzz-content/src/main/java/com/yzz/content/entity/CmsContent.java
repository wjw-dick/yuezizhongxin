package com.yzz.content.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 内容。发布前做适用端校验与敏感词检测。
 */
@Data
@TableName("cms_content")
public class CmsContent {

    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户标识。 */
    private Long tenantId;

    /** 类型。 */
    private String contentType;

    /** 标题。 */
    private String title;

    /** 封面图键。 */
    private String cover;

    /** 富文本正文。 */
    private String bodyHtml;

    /** 适用端。 */
    private String applyEnds;

    /** SEO 标题。 */
    private String seoTitle;

    /** SEO 关键词。 */
    private String seoKeywords;

    /** SEO 描述。 */
    private String seoDescription;

    /** 案例是否已授权。 */
    private Integer authorized;

    /** 状态：草稿/待审核/已发布/已下线。 */
    private String status;

    /** 发布时间。 */
    private LocalDateTime publishedTime;

    /** 创建时间。 */
    private LocalDateTime createdTime;

    /** 更新时间。 */
    private LocalDateTime updatedTime;

    /** 逻辑删除。 */
    @TableLogic
    private Integer deleted;
}
