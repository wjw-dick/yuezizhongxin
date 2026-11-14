package com.yzz.mp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 公众号粉丝。UnionID 去重。
 */
@Data
@TableName("mp_fan")
public class MpFan {

    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户标识。 */
    private Long tenantId;

    /** 公众号标识。 */
    private Long accountId;

    /** 用户标识。 */
    private String openId;

    /** UnionID。 */
    private String unionId;

    /** 昵称。 */
    private String nickname;

    /** 头像。 */
    private String avatar;

    /** 性别。 */
    private String gender;

    /** 地区。 */
    private String region;

    /** 备注。 */
    private String remark;

    /** 订阅状态。 */
    private String subscribeStatus;

    /** 关注时间。 */
    private LocalDateTime subscribeTime;

    /** 取关时间。 */
    private LocalDateTime unsubscribeTime;

    /** 创建时间。 */
    private LocalDateTime createdTime;
}
