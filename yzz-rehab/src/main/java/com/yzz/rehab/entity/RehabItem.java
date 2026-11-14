package com.yzz.rehab.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 康复项目库。
 */
@Data
@TableName("rehab_item")
public class RehabItem {

    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户标识。 */
    private Long tenantId;

    /** 项目名称。 */
    private String name;

    /** 适用阶段（字典中文名）。 */
    private String stage;

    /** 单次时长（分钟）。 */
    private Integer durationMin;

    /** 疗程次数。 */
    private Integer courseTimes;

    /** 价格。 */
    private BigDecimal price;

    /** 人员资质要求。 */
    private String qualification;

    /** 状态：ENABLED/DISABLED。 */
    private String status;

    /** 创建时间。 */
    private LocalDateTime createdTime;
}
