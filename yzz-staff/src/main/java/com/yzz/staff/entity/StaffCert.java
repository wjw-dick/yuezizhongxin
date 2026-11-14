package com.yzz.staff.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 资质证件，到期前 60 天提醒。
 */
@Data
@TableName("staff_cert")
public class StaffCert {

    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户标识。 */
    private Long tenantId;

    /** 人员标识。 */
    private Long staffId;

    /** 证书类型。 */
    private String certType;

    /** 证书编号。 */
    private String certNo;

    /** 扫描件键。 */
    private String fileKey;

    /** 到期日期。 */
    private LocalDate expireDate;

    /** 创建时间。 */
    private LocalDateTime createdTime;
}
