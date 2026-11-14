package com.yzz.tenant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yzz.common.tenant.TenantState;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 平台租户表。租户编码唯一，五类额度随套餐下发，可单独调整并留痕。
 */
@Data
@TableName("platform_tenant")
public class PlatformTenant {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户标识，用于子域名与存储路径，全局唯一。 */
    private String tenantCode;

    private String tenantName;

    private String industry;

    private String contactName;

    private String contactPhone;

    private Long packageId;

    /** 租户状态，取值见 {@link TenantState}。 */
    private String status;

    private LocalDateTime trialEndTime;

    private LocalDateTime expireTime;

    private Integer seatLimit;

    private Integer mpLimit;

    private Integer storageLimitGb;

    private Integer smsLimit;

    private Integer apiLimit;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;

    @TableLogic
    private Integer deleted;

    public boolean isWritable() {
        TenantState state = TenantState.of(status);
        return state.accessible() && !state.writeLimited();
    }
}
