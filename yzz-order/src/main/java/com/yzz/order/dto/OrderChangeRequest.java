package com.yzz.order.dto;

import java.time.LocalDate;

/**
 * 订单变更入参：换房、延期、加项。
 */
public record OrderChangeRequest(
        String changeType,
        Long newRoomId,
        Long newPackageId,
        LocalDate newCheckoutDate,
        String reason) {
}
