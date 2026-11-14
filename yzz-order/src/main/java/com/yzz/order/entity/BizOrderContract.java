package com.yzz.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 订单合同附件。
 */
@Data
@TableName("biz_order_contract")
public class BizOrderContract {

    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户标识。 */
    private Long tenantId;

    /** 订单标识。 */
    private Long orderId;

    /** 合同附件键。 */
    private String fileKey;

    /** 文件名。 */
    private String fileName;

    /** 签署时间。 */
    private LocalDateTime signedTime;

    /** 创建时间。 */
    private LocalDateTime createdTime;
}
