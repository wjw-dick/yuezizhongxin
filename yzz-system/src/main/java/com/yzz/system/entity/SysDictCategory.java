package com.yzz.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 字典分类（平台统一下发，不带 tenant_id）。
 */
@Data
@TableName("sys_dict_category")
public class SysDictCategory {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 分类编码，仅内部使用。 */
    private String category;

    /** 分类中文名，页面展示值。 */
    private String name;

    private Integer sort;

    /** ENABLED / DISABLED。 */
    private String status;
}
