package com.yzz.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 平台菜单表。与租户菜单树物理隔离，只服务平台三角色。
 */
@Data
@TableName("platform_menu")
public class PlatformMenu {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long parentId;

    private String title;

    private String path;

    private String icon;

    /** 菜单类型：目录 / 菜单 / 按钮。 */
    private String menuType;

    private Integer sort;

    private Integer visible;
}
