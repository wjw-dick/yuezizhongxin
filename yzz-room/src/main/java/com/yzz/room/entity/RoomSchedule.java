package com.yzz.room.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 房间档期。同租户同房间同日期唯一，作为并发抢房的数据库兜底。
 */
@Data
@TableName("room_schedule")
public class RoomSchedule {

    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户标识。 */
    private Long tenantId;

    /** 房间标识。 */
    private Long roomId;

    /** 档期日期。 */
    private LocalDate bizDate;

    /** 档期状态：LOCKED/OCCUPIED/CLEANING/MAINTENANCE。 */
    private String state;

    /** 占用订单。 */
    private Long orderId;

    /** 销售预留过期时间。 */
    private LocalDateTime holdExpireTime;

    /** 备注（换房、延期留痕）。 */
    private String remark;

    /** 创建时间。 */
    private LocalDateTime createdTime;

    /** 更新时间。 */
    private LocalDateTime updatedTime;
}
