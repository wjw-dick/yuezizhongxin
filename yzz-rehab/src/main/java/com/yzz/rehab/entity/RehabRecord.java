package com.yzz.rehab.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 康复执行记录。
 */
@Data
@TableName("rehab_record")
public class RehabRecord {

    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户标识。 */
    private Long tenantId;

    /** 预约标识。 */
    private Long appointmentId;

    /** 项目名称。 */
    private String itemName;

    /** 实际时长。 */
    private Integer durationMin;

    /** 治疗师。 */
    private Long therapistId;

    /** 客户反馈。 */
    private String feedback;

    /** 下次预约日期。 */
    private LocalDate nextAppoint;

    /** 创建时间。 */
    private LocalDateTime createdTime;
}
