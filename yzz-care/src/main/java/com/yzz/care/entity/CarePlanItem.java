package com.yzz.care.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 护理计划项。完成时间由服务端写入，不接受客户端时间。
 */
@Data
@TableName("care_plan_item")
public class CarePlanItem {

    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户标识。 */
    private Long tenantId;

    /** 护理计划标识。 */
    private Long planId;

    /** 护理项目。 */
    private String itemName;

    /** 对象：产妇/新生儿。 */
    private String targetType;

    /** 计划日期。 */
    private LocalDate planDate;

    /** 时段。 */
    private String timeSlot;

    /** 执行护理师。 */
    private Long nurseId;

    /** 状态：待执行/已完成/已跳过。 */
    private String status;

    /** 完成时间。 */
    private LocalDateTime finishedTime;
}
