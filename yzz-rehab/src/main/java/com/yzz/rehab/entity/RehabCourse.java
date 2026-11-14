package com.yzz.rehab.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 疗程卡与余额。
 */
@Data
@TableName("rehab_course")
public class RehabCourse {

    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户标识。 */
    private Long tenantId;

    /** 订单标识。 */
    private Long orderId;

    /** 康复项目。 */
    private Long itemId;

    /** 总次数。 */
    private Integer totalTimes;

    /** 已用次数。 */
    private Integer usedTimes;

    /** 有效期。 */
    private LocalDate expireDate;

    /** 来源：套餐赠送/单独购买。 */
    private String source;

    /** 创建时间。 */
    private LocalDateTime createdTime;
}
