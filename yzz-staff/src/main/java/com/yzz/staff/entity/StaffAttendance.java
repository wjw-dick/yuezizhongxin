package com.yzz.staff.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 考勤打卡。
 */
@Data
@TableName("staff_attendance")
public class StaffAttendance {

    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户标识。 */
    private Long tenantId;

    /** 人员标识。 */
    private Long staffId;

    /** 考勤日期。 */
    private LocalDate attendanceDate;

    /** 上班打卡。 */
    private LocalDateTime checkInTime;

    /** 下班打卡。 */
    private LocalDateTime checkOutTime;

    /** 打卡定位。 */
    private String location;

    /** 请假类型。 */
    private String leaveType;

    /** 创建时间。 */
    private LocalDateTime createdTime;
}
