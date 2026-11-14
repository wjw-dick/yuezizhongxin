package com.yzz.stay.controller;

import com.yzz.common.api.R;
import com.yzz.common.tenant.AuthGuard;
import com.yzz.stay.entity.StayCheckin;
import com.yzz.stay.entity.StayCheckout;
import com.yzz.stay.service.StayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 入馆登记与出馆结算（T38、T39）。
 */
@Tag(name = "租户-入馆出馆")
@RestController
@RequestMapping("/api/tenant/stay")
public class StayController {

    private final StayService stayService;

    public StayController(StayService stayService) {
        this.stayService = stayService;
    }

    @Operation(summary = "入馆登记列表")
    @GetMapping("/checkins")
    public R<List<StayCheckin>> checkins(@RequestParam(required = false) String status) {
        AuthGuard.requireTenant();
        return R.ok(stayService.checkins(status));
    }

    @Operation(summary = "办理入馆")
    @PostMapping("/checkins")
    public R<Long> checkin(@RequestBody Map<String, Object> body) {
        AuthGuard.requireTenant();
        StayCheckin checkin = new StayCheckin();
        checkin.setOrderId(Long.valueOf(String.valueOf(body.get("orderId"))));
        checkin.setRoomId(body.get("roomId") == null ? null : Long.valueOf(String.valueOf(body.get("roomId"))));
        checkin.setMotherName(String.valueOf(body.getOrDefault("motherName", "")));
        checkin.setMotherAge(intOrNull(body.get("motherAge")));
        checkin.setGestationalWeek(intOrNull(body.get("gestationalWeek")));
        checkin.setDeliveryType((String) body.getOrDefault("deliveryType", "自然分娩"));
        checkin.setDeliveryDate(body.get("deliveryDate") == null ? null
                : java.time.LocalDate.parse(String.valueOf(body.get("deliveryDate"))));
        checkin.setBabyBirthDate(body.get("babyBirthDate") == null ? null
                : java.time.LocalDate.parse(String.valueOf(body.get("babyBirthDate"))));
        checkin.setBabyWeight(body.get("babyWeight") == null ? null
                : new BigDecimal(String.valueOf(body.get("babyWeight"))));
        checkin.setFeedingType((String) body.get("feedingType"));
        checkin.setFamilyInfo((String) body.get("familyInfo"));
        checkin.setEmergencyContact((String) body.get("emergencyContact"));
        checkin.setIdCardNo((String) body.get("idCardNo"));
        checkin.setHealthNoticeSigned(intOrNull(body.get("healthNoticeSigned")));
        checkin.setDepositAmount(body.get("depositAmount") == null ? null
                : new BigDecimal(String.valueOf(body.get("depositAmount"))));
        int careDays = body.get("careDays") == null ? 28 : Integer.parseInt(String.valueOf(body.get("careDays")));
        return R.ok(stayService.checkin(checkin, careDays));
    }

    @Operation(summary = "在住详情（含订单与护理计划）")
    @GetMapping("/orders/{orderId}")
    public R<Map<String, Object>> detail(@PathVariable Long orderId) {
        AuthGuard.requireTenant();
        return R.ok(stayService.detail(orderId));
    }

    @Operation(summary = "办理出馆")
    @PostMapping("/checkouts")
    public R<Long> checkout(@RequestBody StayCheckout request) {
        AuthGuard.requireTenant();
        return R.ok(stayService.checkout(request));
    }

    @Operation(summary = "出馆结算收款")
    @PostMapping("/orders/{orderId}/settle")
    public R<Map<String, Object>> settle(@PathVariable Long orderId,
                                         @RequestBody Map<String, Object> body) {
        AuthGuard.requireTenant();
        String method = (String) body.getOrDefault("method", "微信支付");
        BigDecimal amount = body.get("amount") == null ? null : new BigDecimal(String.valueOf(body.get("amount")));
        return R.ok(stayService.settle(orderId, method, amount));
    }

    private Integer intOrNull(Object value) {
        return value == null ? null : Integer.valueOf(String.valueOf(value));
    }
}
