package com.yzz.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 数据导出留痕。
 */
@Data
@TableName("sys_export_log")
public class SysExportLog {

    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户标识。 */
    private Long tenantId;

    /** 导出模块。 */
    private String module;

    /** 导出用途。 */
    private String purpose;

    /** 导出行数。 */
    private Integer rowCount;

    /** 水印内容。 */
    private String watermark;

    /** 操作人。 */
    private Long operatorId;

    /** 创建时间。 */
    private LocalDateTime createdTime;
}
