package com.yzz.care.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/**
 * 护理异常单。特急即时推送护理主管。
 */
@Data
@TableName(value = "care_abnormal", autoResultMap = true)
public class CareAbnormal {

    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户标识。 */
    private Long tenantId;

    /** 订单标识。 */
    private Long orderId;

    /** 异常类型（字典中文名）。 */
    private String abnormalType;

    /** 紧急程度：一般/紧急/特急。 */
    private String urgency;

    /** 描述。 */
    private String description;

    /** 照片。 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> photos;

    /** 上报人。 */
    private Long reporterId;

    /** 护理主管。 */
    private Long supervisorId;

    /** 处置记录。 */
    private String handleResult;

    /** 是否告知家属。 */
    private Integer familyNotified;

    /** 状态：待处理/处理中/已闭环。 */
    private String status;

    /** 创建时间。 */
    private LocalDateTime createdTime;

    /** 闭环时间。 */
    private LocalDateTime closedTime;
}
