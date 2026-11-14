package com.yzz.member.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 会员优惠券。
 */
@Data
@TableName("member_coupon")
public class MemberCoupon {

    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户标识。 */
    private Long tenantId;

    /** 会员标识。 */
    private Long memberId;

    /** 优惠券标识。 */
    private Long couponId;

    /** 状态：未使用/已使用/已过期。 */
    private String status;

    /** 使用时间。 */
    private LocalDateTime usedTime;

    /** 核销订单。 */
    private Long orderId;

    /** 创建时间。 */
    private LocalDateTime createdTime;
}
