package com.yzz.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 语言列表（全局，不带 tenant_id）。
 */
@Data
@TableName("sys_language")
public class SysLanguage {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** zh-CN / en-US。 */
    private String code;

    /** 语言中文名。 */
    private String name;

    private Integer isDefault;

    private Integer enabled;

    private Integer sort;
}
