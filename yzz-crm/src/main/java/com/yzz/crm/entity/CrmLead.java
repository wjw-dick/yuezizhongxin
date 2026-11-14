package com.yzz.crm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 销售线索。手机号加密存储，列表展示脱敏。
 */
@Data
@TableName("crm_lead")
public class CrmLead {

    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户标识。 */
    private Long tenantId;

    /** 联系人。 */
    private String customerName;

    /** 手机号（加密存储）。 */
    private String phone;

    /** 预产期。 */
    private LocalDate dueDate;

    /** 意向门店。 */
    private Long storeId;

    /** 意向房型。 */
    private String intendRoomType;

    /** 预算下限。 */
    private BigDecimal budgetMin;

    /** 预算上限。 */
    private BigDecimal budgetMax;

    /** 线索来源（字典中文名）。 */
    private String sourceCode;

    /** 跟进阶段。 */
    private String stage;

    /** 归属销售顾问。 */
    private Long advisorId;

    /** 下次跟进时间。 */
    private LocalDateTime nextFollowTime;

    /** 流失原因。 */
    private String lostReason;

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
