package com.yzz.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 端入口与带参码。
 */
@Data
@TableName("sys_entry")
public class SysEntry {

    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户标识。 */
    private Long tenantId;

    /** 类型。 */
    private String entryType;

    /** 名称。 */
    private String name;

    /** 门店。 */
    private Long storeId;

    /** 渠道参数。 */
    private String channelCode;

    /** 码图键。 */
    private String imageKey;

    /** 链接地址。 */
    private String url;

    /** 扫码次数。 */
    private Integer scanCount;

    /** 创建时间。 */
    private LocalDateTime createdTime;
}
