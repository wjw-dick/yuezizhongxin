package com.yzz.tenant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 平台审计表。与租户审计物理分离，平台操作可按租户筛选。
 */
@Data
@TableName("platform_audit")
public class PlatformAudit {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String module;

    private String action;

    private String target;

    private String detail;

    /** 结果：成功 / 失败。 */
    private String result;

    private Long operatorId;

    private String operatorName;

    private String platformRole;

    private Long tenantId;

    private String tenantCode;

    private String ip;

    private LocalDateTime createdTime;
}
