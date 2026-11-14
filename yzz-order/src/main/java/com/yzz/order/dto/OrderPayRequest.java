package com.yzz.order.dto;

import java.math.BigDecimal;

/**
 * 收款入参。款项类型：定金 / 尾款 / 押金；支付方式：微信支付 / 现金 / 银行转账 / 对公转账。
 */
public record OrderPayRequest(
        String payType,
        String method,
        BigDecimal amount,
        String remark) {
}
