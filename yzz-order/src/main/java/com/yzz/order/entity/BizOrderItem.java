package com.yzz.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import lombok.Data;

/**
 * 订单明细。
 */
@Data
@TableName("biz_order_item")
public class BizOrderItem {

    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户标识。 */
    private Long tenantId;

    /** 订单标识。 */
    private Long orderId;

    /** 类型：套餐/增值服务/商品/康复项目。 */
    private String itemType;

    /** 项目名称。 */
    private String itemName;

    /** 数量。 */
    private Integer quantity;

    /** 单价。 */
    private BigDecimal price;

    /** 金额。 */
    private BigDecimal amount;
}
