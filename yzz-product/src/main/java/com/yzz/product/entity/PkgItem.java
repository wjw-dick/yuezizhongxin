package com.yzz.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import lombok.Data;

/**
 * 套餐包含项。
 */
@Data
@TableName("pkg_item")
public class PkgItem {

    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户标识。 */
    private Long tenantId;

    /** 套餐标识。 */
    private Long packageId;

    /** 类型：康复项目/增值服务。 */
    private String itemType;

    /** 项目名称。 */
    private String itemName;

    /** 含数量。 */
    private Integer quantity;

    /** 单价（元）。 */
    private BigDecimal price;
}
