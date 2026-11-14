package com.yzz.rehab.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 康复预约。治疗师与时段冲突由服务层校验。
 */
@Data
@TableName("rehab_appointment")
public class RehabAppointment {

    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户标识。 */
    private Long tenantId;

    /** 订单标识。 */
    private Long orderId;

    /** 疗程卡标识。 */
    private Long courseId;

    /** 康复室。 */
    private Long roomId;

    /** 治疗师。 */
    private Long therapistId;

    /** 预约日期。 */
    private LocalDate appointDate;

    /** 开始时间。 */
    private String startTime;

    /** 结束时间。 */
    private String endTime;

    /** 状态：已预约/已签到/已取消/已完成。 */
    private String status;

    /** 创建时间。 */
    private LocalDateTime createdTime;
}
