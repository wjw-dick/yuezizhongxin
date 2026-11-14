package com.yzz.member.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 储值记录，预付卡规则公示留痕。
 */
@Data
@TableName("member_recharge")
public class MemberRecharge {

    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户标识。 */
    private Long tenantId;

    /** 会员标识。 */
    private Long memberId;

    /** 储值单号。 */
    private String rechargeNo;

    /** 储值金额。 */
    private BigDecimal amount;

    /** 赠送金额。 */
    private BigDecimal giftAmount;

    /** 状态：已支付/已退款。 */
    private String status;

    /** 是否已展示规则。 */
    private Integer ruleShown;

    /** 创建时间。 */
    private LocalDateTime createdTime;
}
