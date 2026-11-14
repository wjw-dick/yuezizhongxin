package com.yzz.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 租户审计日志。只追加，不提供修改与删除；平台审计单独存放于 platform_audit。
 */
@Data
@TableName("sys_audit_log")
public class SysAuditLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String tenantCode;

    private String module;

    private String action;

    private String target;

    private String detail;

    /** 成功 / 失败。 */
    private String result;

    private Long operatorId;

    private String operatorName;

    private String roleName;

    private String ip;

    private LocalDateTime createdTime;
}
