package com.yzz.care.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/**
 * 护理记录。提交后 24 小时内可改并留痕，超时需主管审批。
 */
@Data
@TableName(value = "care_record", autoResultMap = true)
public class CareRecord {

    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户标识。 */
    private Long tenantId;

    /** 护理计划项标识。 */
    private Long planItemId;

    /** 订单标识。 */
    private Long orderId;

    /** 对象：产妇/新生儿。 */
    private String targetType;

    /** 护理项目。 */
    private String itemName;

    /** 体温（℃）。 */
    private BigDecimal temperature;

    /** 血压。 */
    private String bloodPressure;

    /** 脉搏。 */
    private Integer pulse;

    /** 体重（kg）。 */
    private BigDecimal weight;

    /** 喂养量（ml）。 */
    private BigDecimal feedingAmount;

    /** 黄疸测值。 */
    private BigDecimal jaundiceValue;

    /** 大小便次数。 */
    private Integer stoolCount;

    /** 记录内容。 */
    private String content;

    /** 照片。 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> photos;

    /** 提交人。 */
    private Long nurseId;

    /** 提交时间（服务端）。 */
    private LocalDateTime submitTime;

    /** 补录原因。 */
    private String backfillReason;

    /** 状态：已提交/待审批。 */
    private String status;

    /** 创建时间。 */
    private LocalDateTime createdTime;
}
