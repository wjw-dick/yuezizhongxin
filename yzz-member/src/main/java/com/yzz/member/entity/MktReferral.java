package com.yzz.member.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 转介绍奖励。
 */
@Data
@TableName("mkt_referral")
public class MktReferral {

    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户标识。 */
    private Long tenantId;

    /** 推荐人会员标识。 */
    private Long referrerMember;

    /** 被推荐线索。 */
    private Long referredLead;

    /** 奖励积分。 */
    private Integer rewardPoints;

    /** 奖励金额。 */
    private BigDecimal rewardAmount;

    /** 状态：待结算/已结算。 */
    private String status;

    /** 创建时间。 */
    private LocalDateTime createdTime;
}
