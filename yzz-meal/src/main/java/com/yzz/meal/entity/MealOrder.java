package com.yzz.meal.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 每日出餐。
 */
@Data
@TableName("meal_order")
public class MealOrder {

    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户标识。 */
    private Long tenantId;

    /** 订单标识。 */
    private Long orderId;

    /** 房间。 */
    private Long roomId;

    /** 用餐日期。 */
    private LocalDate mealDate;

    /** 餐次。 */
    private String mealType;

    /** 实际菜品。 */
    private String dishes;

    /** 特殊标记。 */
    private String specialTag;

    /** 加餐收费。 */
    private BigDecimal extraCharge;

    /** 是否出餐核对。 */
    private Integer checked;

    /** 异常回传。 */
    private String abnormal;

    /** 创建时间。 */
    private LocalDateTime createdTime;
}
