package com.yzz.crm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 线索跟进记录。
 */
@Data
@TableName("crm_follow")
public class CrmFollow {

    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户标识。 */
    private Long tenantId;

    /** 线索标识。 */
    private Long leadId;

    /** 跟进时间。 */
    private LocalDateTime followTime;

    /** 沟通内容。 */
    private String content;

    /** 通话结果。 */
    private String result;

    /** 下次提醒时间。 */
    private LocalDateTime nextTime;

    /** 跟进人。 */
    private Long operatorId;

    /** 创建时间。 */
    private LocalDateTime createdTime;
}
