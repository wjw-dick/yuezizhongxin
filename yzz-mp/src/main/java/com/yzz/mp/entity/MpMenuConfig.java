package com.yzz.mp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 公众号菜单配置（一级≤3、二级≤5）。
 */
@Data
@TableName("mp_menu_config")
public class MpMenuConfig {

    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户标识。 */
    private Long tenantId;

    /** 公众号标识。 */
    private Long accountId;

    /** 配置名称。 */
    private String configName;

    /** 菜单树 JSON。 */
    private String treeJson;

    /** 是否已发布。 */
    private Integer published;

    /** 发布时间。 */
    private LocalDateTime publishTime;

    /** 创建时间。 */
    private LocalDateTime createdTime;
}
