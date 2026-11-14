package com.yzz.stay.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 出馆结算。
 */
@Data
@TableName("stay_checkout")
public class StayCheckout {

    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户标识。 */
    private Long tenantId;

    /** 订单标识。 */
    private Long orderId;

    /** 入馆登记标识。 */
    private Long checkinId;

    /** 尾款。 */
    private BigDecimal tailAmount;

    /** 增值消费。 */
    private BigDecimal extraAmount;

    /** 退还押金。 */
    private BigDecimal depositRefund;

    /** 物品是否归还齐全。 */
    private Integer itemReturned;

    /** 出馆小结。 */
    private String summary;

    /** 满意度评分。 */
    private Integer satisfaction;

    /** 42 天复查预约日期。 */
    private LocalDate reviewAppointment;

    /** 出馆时间。 */
    private LocalDateTime checkoutTime;

    /** 经办人。 */
    private Long operatorId;

    /** 创建时间。 */
    private LocalDateTime createdTime;
}
