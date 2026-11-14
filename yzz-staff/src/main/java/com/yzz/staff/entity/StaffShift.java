package com.yzz.staff.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 班次定义。
 */
@Data
@TableName("staff_shift")
public class StaffShift {

    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户标识。 */
    private Long tenantId;

    /** 班次名称。 */
    private String shiftName;

    /** 开始时间。 */
    private String startTime;

    /** 结束时间。 */
    private String endTime;

    /** 是否跨天。 */
    private Integer crossDay;

    /** 创建时间。 */
    private LocalDateTime createdTime;
}
