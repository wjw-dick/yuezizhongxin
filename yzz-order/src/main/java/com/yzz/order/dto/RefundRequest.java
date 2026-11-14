package com.yzz.order.dto;

import java.math.BigDecimal;

/**
 * 退订申请与审核入参。
 */
public record RefundRequest(
        BigDecimal amount,
        BigDecimal penaltyAmount,
        String reasonCode,
        String reason,
        Boolean approved,
        String auditRemark) {
}
