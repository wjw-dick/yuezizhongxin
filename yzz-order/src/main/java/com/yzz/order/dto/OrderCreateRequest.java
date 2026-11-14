package com.yzz.order.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 下单入参（后台代客下单与小程序共用）。
 */
public record OrderCreateRequest(
        Long leadId,
        String customerName,
        String phone,
        Long packageId,
        Long roomTypeId,
        Long roomId,
        LocalDate checkinDate,
        LocalDate checkoutDate,
        String careMode,
        Integer companionCount,
        BigDecimal bondAmount,
        BigDecimal discountAmount,
        String remark,
        String source,
        List<Item> extras,
        List<Item> products) {

    /** 增值服务或商品行。 */
    public record Item(String itemType, String itemName, Long productId, Long skuId,
                       Integer quantity, BigDecimal price) {
    }
}
