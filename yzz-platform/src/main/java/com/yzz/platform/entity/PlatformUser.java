package com.yzz.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 平台账号表。与租户账号（sys_user）物理分离，登录入口不同。
 */
@Data
@TableName("platform_user")
public class PlatformUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    /** BCrypt 散列。 */
    private String password;

    private String realName;

    /** 平台角色编码：platform_super / platform_oper / platform_fin。 */
    private String roleCode;

    /** ENABLED / DISABLED。 */
    private String status;

    private LocalDateTime lastLoginTime;

    private LocalDateTime createdTime;
}
