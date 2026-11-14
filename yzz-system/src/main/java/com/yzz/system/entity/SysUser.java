package com.yzz.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 租户账号表，全表带 tenant_id，(tenant_id, username) 唯一。
 */
@Data
@TableName("sys_user")
public class SysUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String username;

    /** BCrypt 散列。 */
    private String password;

    private String realName;

    private String phone;

    private Long roleId;

    /** ENABLED / DISABLED。 */
    private String status;

    private LocalDateTime lastLoginTime;

    private LocalDateTime createdTime;
}
