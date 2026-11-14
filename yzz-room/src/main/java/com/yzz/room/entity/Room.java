package com.yzz.room.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 房间台账。房态五态由档期与入馆出馆联动维护。
 */
@Data
@TableName("room")
public class Room {

    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户标识。 */
    private Long tenantId;

    /** 房间编号。 */
    private String roomNo;

    /** 房型标识。 */
    private Long roomTypeId;

    /** 楼层。 */
    private String floor;

    /** 房态：AVAILABLE/LOCKED/OCCUPIED/CLEANING/MAINTENANCE。 */
    private String status;

    /** 备注。 */
    private String remark;

    /** 创建时间。 */
    private LocalDateTime createdTime;

    /** 更新时间。 */
    private LocalDateTime updatedTime;

    /** 逻辑删除。 */
    @TableLogic
    private Integer deleted;
}
