package com.yzz.meal.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 月子餐阶段模板。
 */
@Data
@TableName("meal_template")
public class MealTemplate {

    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户标识。 */
    private Long tenantId;

    /** 餐食阶段。 */
    private String stage;

    /** 阶段内第几天。 */
    private Integer dayIndex;

    /** 餐次。 */
    private String mealType;

    /** 菜品清单。 */
    private String dishes;

    /** 本餐忌口标签。 */
    private String avoidTags;

    /** 创建时间。 */
    private LocalDateTime createdTime;
}
