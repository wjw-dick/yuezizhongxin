package com.yzz.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import lombok.Data;

/**
 * 商品规格。
 */
@Data
@TableName("prod_sku")
public class ProdSku {

    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户标识。 */
    private Long tenantId;

    /** 商品标识。 */
    private Long productId;

    /** 规格名称。 */
    private String skuName;

    /** 规格售价。 */
    private BigDecimal price;

    /** 规格库存。 */
    private Integer stock;
}
