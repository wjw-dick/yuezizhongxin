package com.yzz.tenant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 平台套餐表。计费维度为席位数，账单按「套餐定额 + 超出席位按量补差」生成。
 */
@Data
@TableName("platform_package")
public class PlatformPackage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String packageName;

    private BigDecimal price;

    /** 计费周期：月 / 年。 */
    private String billingCycle;

    private Integer endCount;

    private Integer seatQuota;

    private Integer mpQuota;

    private Integer storageQuotaGb;

    private Integer smsQuota;

    private Integer apiQuota;

    /** 是否支持试用：1 支持，0 不支持。 */
    private Integer trialSupported;

    private String remark;

    private LocalDateTime createdTime;
}
