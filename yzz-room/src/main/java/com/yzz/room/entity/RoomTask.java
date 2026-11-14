package com.yzz.room.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 房间清洁与维修任务。
 */
@Data
@TableName("room_task")
public class RoomTask {

    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户标识。 */
    private Long tenantId;

    /** 房间标识。 */
    private Long roomId;

    /** 类型：清洁/维修。 */
    private String taskType;

    /** 状态：待处理/进行中/已完成。 */
    private String status;

    /** 执行人。 */
    private Long assigneeId;

    /** 要求完成时间。 */
    private LocalDateTime dueTime;

    /** 任务说明。 */
    private String remark;

    /** 完成时间。 */
    private LocalDateTime finishedTime;

    /** 创建时间。 */
    private LocalDateTime createdTime;
}
