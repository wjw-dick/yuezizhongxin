package com.yzz.care.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 交接班。双方签名后生效，未完成事项自动延续。
 */
@Data
@TableName("care_handover")
public class CareHandover {

    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户标识。 */
    private Long tenantId;

    /** 班次日期。 */
    private LocalDate shiftDate;

    /** 班次：早班/中班/夜班。 */
    private String shiftType;

    /** 交班人。 */
    private Long fromNurseId;

    /** 接班人。 */
    private Long toNurseId;

    /** 重点观察对象。 */
    private String watchPoints;

    /** 未完成事项。 */
    private String unfinished;

    /** 异常延续。 */
    private String abnormalNotes;

    /** 交班签名图键。 */
    private String fromSign;

    /** 接班签名图键。 */
    private String toSign;

    /** 接班确认时间。 */
    private LocalDateTime confirmedTime;

    /** 状态：待确认/已确认。 */
    private String status;

    /** 创建时间。 */
    private LocalDateTime createdTime;
}
