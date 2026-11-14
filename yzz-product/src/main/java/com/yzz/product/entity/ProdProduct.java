package com.yzz.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/**
 * 商品。
 */
@Data
@TableName(value = "prod_product", autoResultMap = true)
public class ProdProduct {

    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户标识。 */
    private Long tenantId;

    /** 商品名称。 */
    private String name;

    /** 分类。 */
    private String category;

    /** 售价（元）。 */
    private BigDecimal price;

    /** 库存。 */
    private Integer stock;

    /** 商品图。 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> images;

    /** 详情富文本。 */
    private String detailHtml;

    /** 状态：ON_SHELF/OFF_SHELF。 */
    private String status;

    /** 创建时间。 */
    private LocalDateTime createdTime;

    /** 更新时间。 */
    private LocalDateTime updatedTime;

    /** 逻辑删除。 */
    @TableLogic
    private Integer deleted;
}
