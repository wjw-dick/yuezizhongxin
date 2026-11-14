package com.yzz.mp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 公众号标签。
 */
@Data
@TableName("mp_tag")
public class MpTag {

    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户标识。 */
    private Long tenantId;

    /** 公众号标识。 */
    private Long accountId;

    /** 标签名称。 */
    private String tagName;

    /** 粉丝数。 */
    private Integer fanCount;

    /** 同步状态。 */
    private String syncStatus;

    /** 创建时间。 */
    private LocalDateTime createdTime;
}
