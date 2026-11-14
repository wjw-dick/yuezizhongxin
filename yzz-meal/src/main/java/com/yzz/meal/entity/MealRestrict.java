package com.yzz.meal.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 忌口与过敏原。
 */
@Data
@TableName("meal_restrict")
public class MealRestrict {

    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户标识。 */
    private Long tenantId;

    /** 订单标识。 */
    private Long orderId;

    /** 忌口类型。 */
    private String restrictType;

    /** 忌口内容/过敏原。 */
    private String content;

    /** 创建时间。 */
    private LocalDateTime createdTime;
}
