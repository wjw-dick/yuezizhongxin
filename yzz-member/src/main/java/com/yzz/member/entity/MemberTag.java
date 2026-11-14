package com.yzz.member.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 会员标签。
 */
@Data
@TableName("member_tag")
public class MemberTag {

    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户标识。 */
    private Long tenantId;

    /** 标签名称。 */
    private String name;

    /** 创建时间。 */
    private LocalDateTime createdTime;
}
