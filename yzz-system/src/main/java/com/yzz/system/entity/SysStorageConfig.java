package com.yzz.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 租户对象存储配置。单租户单条，密钥加密存储。
 */
@Data
@TableName("sys_storage_config")
public class SysStorageConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    /** local / s3 / minio / oss / cos。 */
    private String driver;

    /** 驱动参数 JSON：endpoint、region、bucket、accessKey、secretKey（加密）、pathStyle。 */
    private String configJson;

    /** 连接测试通过才允许保存生效。 */
    private Integer tested;

    private LocalDateTime updatedTime;
}
