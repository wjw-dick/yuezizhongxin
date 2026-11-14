package com.yzz.member.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 会员。UnionID 为多端唯一口径。
 */
@Data
@TableName("member_info")
public class MemberInfo {

    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户标识。 */
    private Long tenantId;

    /** 微信 UnionID。 */
    private String unionId;

    /** OpenID。 */
    private String openId;

    /** 手机号（加密）。 */
    private String phone;

    /** 昵称。 */
    private String nickname;

    /** 头像。 */
    private String avatar;

    /** 来源渠道。 */
    private String source;

    /** 积分余额。 */
    private Integer points;

    /** 储值余额。 */
    private BigDecimal balance;

    /** 创建时间。 */
    private LocalDateTime createdTime;

    /** 更新时间。 */
    private LocalDateTime updatedTime;

    /** 逻辑删除。 */
    @TableLogic
    private Integer deleted;
}
