package com.yzz.member.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 营销活动（拼团/限时优惠）。
 */
@Data
@TableName("mkt_activity")
public class MktActivity {

    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户标识。 */
    private Long tenantId;

    /** 活动名称。 */
    private String name;

    /** 类型。 */
    private String activityType;

    /** 活动规则。 */
    private String ruleJson;

    /** 开始时间。 */
    private LocalDateTime startTime;

    /** 结束时间。 */
    private LocalDateTime endTime;

    /** 状态。 */
    private String status;

    /** 参与人数。 */
    private Integer joinCount;

    /** 创建时间。 */
    private LocalDateTime createdTime;
}
