package com.yzz.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 订单。金额构成拆为套餐/增值/商品/优惠四段，便于对账。
 */
@Data
@TableName("biz_order")
public class BizOrder {

    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户标识。 */
    private Long tenantId;

    /** 订单编号。 */
    private String orderNo;

    /** 来源线索。 */
    private Long leadId;

    /** 客户姓名。 */
    private String customerName;

    /** 客户手机号（加密）。 */
    private String phone;

    /** 套餐标识。 */
    private Long packageId;

    /** 房型标识。 */
    private Long roomTypeId;

    /** 房间标识。 */
    private Long roomId;

    /** 入住日期。 */
    private LocalDate checkinDate;

    /** 出馆日期。 */
    private LocalDate checkoutDate;

    /** 护理模式。 */
    private String careMode;

    /** 陪护人数。 */
    private Integer companionCount;

    /** 套餐费。 */
    private BigDecimal packageAmount;

    /** 增值服务费。 */
    private BigDecimal extraAmount;

    /** 商品费。 */
    private BigDecimal productAmount;

    /** 优惠金额。 */
    private BigDecimal discountAmount;

    /** 应收总额。 */
    private BigDecimal totalAmount;

    /** 定金。 */
    private BigDecimal depositAmount;

    /** 尾款。 */
    private BigDecimal tailAmount;

    /** 押金。 */
    private BigDecimal bondAmount;

    /** 已收金额。 */
    private BigDecimal paidAmount;

    /** 订单状态。 */
    private String status;

    /** 订单来源。 */
    private String source;

    /** 签约方式。 */
    private String signType;

    /** 备注。 */
    private String remark;

    /** 创建人。 */
    private Long createdBy;

    /** 创建时间。 */
    private LocalDateTime createdTime;

    /** 更新时间。 */
    private LocalDateTime updatedTime;

    /** 逻辑删除。 */
    @TableLogic
    private Integer deleted;
}
