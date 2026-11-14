package com.yzz.member.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 积分流水。
 */
@Data
@TableName("member_point_log")
public class MemberPointLog {

    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户标识。 */
    private Long tenantId;

    /** 会员标识。 */
    private Long memberId;

    /** 变动值。 */
    private Integer changeValue;

    /** 变动原因。 */
    private String reason;

    /** 创建时间。 */
    private LocalDateTime createdTime;
}
