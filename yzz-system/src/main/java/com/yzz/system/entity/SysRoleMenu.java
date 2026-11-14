package com.yzz.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 角色菜单关联。
 */
@Data
@TableName("sys_role_menu")
public class SysRoleMenu {

    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户标识。 */
    private Long tenantId;

    /** 角色标识。 */
    private Long roleId;

    /** 菜单标识。 */
    private Long menuId;
}
