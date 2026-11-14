package com.yzz.staff.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 排班。
 */
@Data
@TableName("staff_schedule")
public class StaffSchedule {

    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户标识。 */
    private Long tenantId;

    /** 人员标识。 */
    private Long staffId;

    /** 排班日期。 */
    private LocalDate scheduleDate;

    /** 班次。 */
    private String shiftType;

    /** 楼层。 */
    private String floor;

    /** 一对一负责订单。 */
    private Long orderId;

    /** 创建时间。 */
    private LocalDateTime createdTime;
}
