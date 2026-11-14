package com.yzz.member.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 会员收货地址。
 */
@Data
@TableName("member_address")
public class MemberAddress {

    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户标识。 */
    private Long tenantId;

    /** 会员标识。 */
    private Long memberId;

    /** 收货人。 */
    private String receiver;

    /** 手机号（加密）。 */
    private String phone;

    /** 省市区。 */
    private String region;

    /** 详细地址。 */
    private String detail;

    /** 是否默认。 */
    private Integer isDefault;

    /** 创建时间。 */
    private LocalDateTime createdTime;
}
