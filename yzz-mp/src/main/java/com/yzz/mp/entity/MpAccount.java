package com.yzz.mp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 公众号台账与凭证。密钥加密，接口只回掩码。
 */
@Data
@TableName("mp_account")
public class MpAccount {

    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户标识。 */
    private Long tenantId;

    /** 公众号名称。 */
    private String accountName;

    /** 微信号。 */
    private String wechatId;

    /** AppID。 */
    private String appId;

    /** AppSecret（加密）。 */
    private String appSecret;

    /** 服务器地址。 */
    private String serverUrl;

    /** 消息令牌（加密）。 */
    private String token;

    /** 加解密密钥（加密）。 */
    private String aesKey;

    /** 网页授权域名。 */
    private String oauthDomain;

    /** JS 安全域名。 */
    private String jsSafeDomain;

    /** 公众号类型。 */
    private String accountType;

    /** 是否已认证。 */
    private Integer verified;

    /** 二维码图键。 */
    private String qrcode;

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
