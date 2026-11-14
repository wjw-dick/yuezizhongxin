package com.yzz.tenant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 平台账单表。账单状态：待支付 / 已支付 / 已逾期（中文名由字典输出）。
 */
@Data
@TableName("platform_bill")
public class PlatformBill {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    /** 账期，形如 2026-11。 */
    private String billPeriod;

    private String itemName;

    private Integer quantity;

    private BigDecimal amount;

    /** 账单状态：PENDING/PAID/OVERDUE。 */
    private String status;

    private LocalDateTime payTime;

    private LocalDateTime createdTime;
}
