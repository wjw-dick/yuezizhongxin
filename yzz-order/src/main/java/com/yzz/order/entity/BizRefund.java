package com.yzz.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 退款单。违约金与可退金额由规则计算，审批后执行。
 */
@Data
@TableName("biz_refund")
public class BizRefund {

    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户标识。 */
    private Long tenantId;

    /** 订单标识。 */
    private Long orderId;

    /** 退款单号。 */
    private String refundNo;

    /** 退款金额。 */
    private BigDecimal amount;

    /** 违约金。 */
    private BigDecimal penaltyAmount;

    /** 退款原因（字典中文名）。 */
    private String reasonCode;

    /** 补充说明。 */
    private String reason;

    /** 状态：待审核/已通过/已驳回/已退款。 */
    private String status;

    /** 审核人。 */
    private Long auditBy;

    /** 审核时间。 */
    private LocalDateTime auditTime;

    /** 创建时间。 */
    private LocalDateTime createdTime;
}
