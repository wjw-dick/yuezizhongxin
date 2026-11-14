package com.yzz.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 收款单。
 */
@Data
@TableName("biz_payment")
public class BizPayment {

    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户标识。 */
    private Long tenantId;

    /** 订单标识。 */
    private Long orderId;

    /** 收款单号。 */
    private String paymentNo;

    /** 款项类型：定金/尾款/押金。 */
    private String payType;

    /** 支付方式。 */
    private String method;

    /** 金额。 */
    private BigDecimal amount;

    /** 状态：待支付/已支付/支付异常/已退款。 */
    private String status;

    /** 微信支付交易号。 */
    private String tradeNo;

    /** 支付时间。 */
    private LocalDateTime paidTime;

    /** 收款人。 */
    private Long operatorId;

    /** 备注。 */
    private String remark;

    /** 创建时间。 */
    private LocalDateTime createdTime;
}
