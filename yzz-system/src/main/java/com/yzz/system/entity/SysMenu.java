package com.yzz.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 租户端菜单与按钮权限。
 */
@Data
@TableName("sys_menu")
public class SysMenu {

    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户标识。 */
    private Long tenantId;

    /** 上级菜单。 */
    private Long parentId;

    /** 菜单标题。 */
    private String title;

    /** 类型：目录/菜单/按钮。 */
    private String menuType;

    /** 路由路径。 */
    private String path;

    /** 权限标识。 */
    private String perm;

    /** 图标。 */
    private String icon;

    /** 排序。 */
    private Integer sort;

    /** 是否可见。 */
    private Integer visible;
}
