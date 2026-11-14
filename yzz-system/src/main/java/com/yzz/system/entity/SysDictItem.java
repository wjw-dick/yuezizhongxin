package com.yzz.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 字典项。中文名称是页面唯一出口，编码只用于接口取值，不渲染。
 */
@Data
@TableName("sys_dict_item")
public class SysDictItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String category;

    private String itemCode;

    private String itemName;

    /** 颜色标记，房态与状态标签使用。 */
    private String color;

    private Integer sort;

    private String status;
}
