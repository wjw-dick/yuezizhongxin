package com.yzz.stay.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 入馆登记。证件号加密存储，健康告知必须签署。
 */
@Data
@TableName("stay_checkin")
public class StayCheckin {

    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户标识。 */
    private Long tenantId;

    /** 订单标识。 */
    private Long orderId;

    /** 房间标识。 */
    private Long roomId;

    /** 产妇姓名。 */
    private String motherName;

    /** 产妇年龄。 */
    private Integer motherAge;

    /** 孕周。 */
    private Integer gestationalWeek;

    /** 分娩方式。 */
    private String deliveryType;

    /** 分娩日期。 */
    private LocalDate deliveryDate;

    /** 新生儿出生日期。 */
    private LocalDate babyBirthDate;

    /** 新生儿体重（kg）。 */
    private BigDecimal babyWeight;

    /** 喂养方式。 */
    private String feedingType;

    /** 家属与陪护人。 */
    private String familyInfo;

    /** 紧急联系人。 */
    private String emergencyContact;

    /** 证件号（加密）。 */
    private String idCardNo;

    /** 健康告知是否签署。 */
    private Integer healthNoticeSigned;

    /** 押金金额。 */
    private BigDecimal depositAmount;

    /** 入馆时间。 */
    private LocalDateTime checkinTime;

    /** 状态：在住/已出馆。 */
    private String status;

    /** 创建时间。 */
    private LocalDateTime createdTime;
}
