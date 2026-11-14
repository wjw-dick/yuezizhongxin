package com.yzz.tenant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

/**
 * 平台租户用量表，按日汇总，用于配额拦截与账单生成。
 */
@Data
@TableName("platform_tenant_usage")
public class PlatformTenantUsage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Integer usedSeat;

    private Integer usedMp;

    /** 已用存储，单位 GB，保留两位小数存整数分位。 */
    private Integer usedStorageGb;

    private Integer usedSms;

    private Integer usedApi;

    private LocalDate statDate;
}
