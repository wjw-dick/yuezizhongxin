package com.yzz.room.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/**
 * 房型。图片、配置与富文本详情用于官网与小程序展示。
 */
@Data
@TableName(value = "room_type", autoResultMap = true)
public class RoomType {

    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户标识。 */
    private Long tenantId;

    /** 房型名称。 */
    private String name;

    /** 房型档位：温馨单间/豪华套间/尊享套房。 */
    private String level;

    /** 面积（㎡）。 */
    private BigDecimal area;

    /** 可住人数。 */
    private Integer maxGuest;

    /** 床型。 */
    private String bedType;

    /** 房间配置。 */
    private String facilities;

    /** 实景图集（对象存储键）。 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> images;

    /** 房型介绍（富文本）。 */
    private String description;

    /** 状态：ENABLED/DISABLED。 */
    private String status;

    /** 排序。 */
    private Integer sort;

    /** 创建时间。 */
    private LocalDateTime createdTime;

    /** 更新时间。 */
    private LocalDateTime updatedTime;

    /** 逻辑删除。 */
    @TableLogic
    private Integer deleted;
}
