package com.yzz.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 套餐。价格为套餐总价，定金按比例计算。
 */
@Data
@TableName("pkg_package")
public class PkgPackage {

    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户标识。 */
    private Long tenantId;

    /** 套餐名称。 */
    private String name;

    /** 套餐天数。 */
    private Integer days;

    /** 适用房型档位。 */
    private String roomTypeLevel;

    /** 护理模式。 */
    private String careMode;

    /** 月子餐标准。 */
    private String mealStandard;

    /** 含康复项目清单。 */
    private String rehabItems;

    /** 套餐价格（元）。 */
    private BigDecimal price;

    /** 定金比例（%）。 */
    private BigDecimal depositRatio;

    /** 可售门店标识，逗号分隔。 */
    private String storeIds;

    /** 状态：ON_SHELF/OFF_SHELF。 */
    private String status;

    /** 排序。 */
    private Integer sort;

    /** 套餐详情富文本。 */
    private String detailHtml;

    /** 创建时间。 */
    private LocalDateTime createdTime;

    /** 更新时间。 */
    private LocalDateTime updatedTime;

    /** 逻辑删除。 */
    @TableLogic
    private Integer deleted;
}
