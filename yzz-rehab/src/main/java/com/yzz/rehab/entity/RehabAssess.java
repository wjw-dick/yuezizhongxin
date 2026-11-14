package com.yzz.rehab.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 产后康复评估。
 */
@Data
@TableName("rehab_assess")
public class RehabAssess {

    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户标识。 */
    private Long tenantId;

    /** 订单标识。 */
    private Long orderId;

    /** 盆底肌评分。 */
    private Integer pelvicScore;

    /** 腹直肌分离（cm）。 */
    private BigDecimal diastasisCm;

    /** 体态评估。 */
    private String posture;

    /** 情绪量表得分。 */
    private Integer moodScore;

    /** 评估报告与建议疗程。 */
    private String report;

    /** 评估师。 */
    private Long assessorId;

    /** 评估时间。 */
    private LocalDateTime assessTime;

    /** 创建时间。 */
    private LocalDateTime createdTime;
}
