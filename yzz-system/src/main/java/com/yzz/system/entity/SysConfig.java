package com.yzz.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 租户系统参数。保存即时生效，金额与比例变更需二次确认并写审计。
 */
@Data
@TableName("sys_config")
public class SysConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    /** 分组中文名：站点基础 / 门店信息 / 订单规则 / 护理规则 / 营销规则 / 消息。 */
    private String configGroup;

    private String configKey;

    private String configValue;

    /** string / number / boolean / json。 */
    private String valueType;

    private String remark;

    private LocalDateTime updatedTime;
}
