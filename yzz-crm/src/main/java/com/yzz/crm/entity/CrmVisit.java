package com.yzz.crm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 参观预约。
 */
@Data
@TableName("crm_visit")
public class CrmVisit {

    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户标识。 */
    private Long tenantId;

    /** 线索标识。 */
    private Long leadId;

    /** 参观日期。 */
    private LocalDate visitDate;

    /** 时段。 */
    private String timeSlot;

    /** 到店人数。 */
    private Integer personCount;

    /** 接待顾问。 */
    private Long advisorId;

    /** 是否到店签到。 */
    private Integer checkedIn;

    /** 参观后评价。 */
    private String feedback;

    /** 创建时间。 */
    private LocalDateTime createdTime;
}
