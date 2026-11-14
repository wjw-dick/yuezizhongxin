package com.yzz.member.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 会员标签关联。
 */
@Data
@TableName("member_tag_rel")
public class MemberTagRel {

    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户标识。 */
    private Long tenantId;

    /** 会员标识。 */
    private Long memberId;

    /** 标签标识。 */
    private Long tagId;
}
