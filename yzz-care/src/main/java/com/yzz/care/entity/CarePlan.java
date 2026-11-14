package com.yzz.care.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 护理计划。按套餐与分娩方式生成默认模板后下发护理师端。
 */
@Data
@TableName("care_plan")
public class CarePlan {

    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户标识。 */
    private Long tenantId;

    /** 订单标识。 */
    private Long orderId;

    /** 客户姓名。 */
    private String customerName;

    /** 房间标识。 */
    private Long roomId;

    /** 护理模式。 */
    private String careMode;

    /** 模板编码。 */
    private String templateCode;

    /** 状态：执行中/已结束。 */
    private String status;

    /** 创建时间。 */
    private LocalDateTime createdTime;

    /** 更新时间。 */
    private LocalDateTime updatedTime;
}
