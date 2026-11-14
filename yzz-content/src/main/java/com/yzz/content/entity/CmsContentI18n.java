package com.yzz.content.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 内容多语言。
 */
@Data
@TableName("cms_content_i18n")
public class CmsContentI18n {

    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户标识。 */
    private Long tenantId;

    /** 内容标识。 */
    private Long contentId;

    /** 语言编码。 */
    private String langCode;

    /** 标题。 */
    private String title;

    /** 正文。 */
    private String bodyHtml;

    /** 是否已翻译。 */
    private Integer translated;
}
