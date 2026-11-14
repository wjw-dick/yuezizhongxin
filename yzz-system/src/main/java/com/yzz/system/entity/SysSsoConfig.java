package com.yzz.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * SSO 配置：单租户单条，支持 CAS / OAuth2 / OIDC / Sa-Token 四种协议。
 */
@Data
@TableName("sys_sso_config")
public class SysSsoConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Integer enabled;

    /** CAS / OAuth2 / OIDC / Sa-Token。 */
    private String protocol;

    /** 只填根地址，授权、令牌、校验地址由服务端拼接。 */
    private String serverUrl;

    private String serviceUrl;

    private String clientId;

    /** AES 加密，接口只回掩码。 */
    private String clientSecret;

    private String accountMapping;

    /** 故障时是否降级为账号密码登录。 */
    private Integer fallbackEnabled;

    /** 账号不存在时的策略：拒绝 / 自动创建。 */
    private String rejectStrategy;

    private LocalDateTime updatedTime;
}
