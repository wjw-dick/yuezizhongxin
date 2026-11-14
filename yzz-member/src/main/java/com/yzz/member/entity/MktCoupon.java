package com.yzz.member.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 优惠券模板。
 */
@Data
@TableName("mkt_coupon")
public class MktCoupon {

    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户标识。 */
    private Long tenantId;

    /** 优惠券名称。 */
    private String name;

    /** 类型。 */
    private String couponType;

    /** 优惠金额。 */
    private BigDecimal amount;

    /** 使用门槛。 */
    private BigDecimal threshold;

    /** 发行总量。 */
    private Integer totalCount;

    /** 已发放数量。 */
    private Integer issuedCount;

    /** 生效时间。 */
    private LocalDateTime startTime;

    /** 失效时间。 */
    private LocalDateTime endTime;

    /** 状态。 */
    private String status;

    /** 创建时间。 */
    private LocalDateTime createdTime;
}
