package com.yzz.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 订单变更留痕（换房/延期/加项/改价/退订）。
 */
@Data
@TableName("biz_order_change")
public class BizOrderChange {

    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户标识。 */
    private Long tenantId;

    /** 订单标识。 */
    private Long orderId;

    /** 变更类型。 */
    private String changeType;

    /** 变更前快照。 */
    private String beforeJson;

    /** 变更后快照。 */
    private String afterJson;

    /** 金额差额。 */
    private BigDecimal amountDiff;

    /** 变更原因。 */
    private String reason;

    /** 操作人。 */
    private Long operatorId;

    /** 创建时间。 */
    private LocalDateTime createdTime;
}
