package com.yzz.care.controller;

import com.yzz.care.entity.CareAbnormal;
import com.yzz.care.entity.CareHandover;
import com.yzz.care.entity.CarePlan;
import com.yzz.care.entity.CarePlanItem;
import com.yzz.care.entity.CareRecord;
import com.yzz.care.service.CareService;
import com.yzz.common.api.R;
import com.yzz.common.tenant.AuthGuard;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 护理计划、记录、交接班、异常与看板（T40–T43、T55）。
 */
@Tag(name = "租户-护理")
@RestController
@RequestMapping("/api/tenant/care")
public class CareController {

    private final CareService careService;

    public CareController(CareService careService) {
        this.careService = careService;
    }

    @Operation(summary = "护理计划列表")
    @GetMapping("/plans")
    public R<List<CarePlan>> plans(@RequestParam(required = false) String status,
                                   @RequestParam(required = false) Long orderId) {
        AuthGuard.requireTenant();
        return R.ok(careService.plans(status, orderId));
    }

    @Operation(summary = "创建护理计划（按套餐与分娩方式生成默认模板）")
    @PostMapping("/plans")
    public R<Long> createPlan(@RequestBody Map<String, Object> body) {
        AuthGuard.requireTenant();
        CarePlan plan = new CarePlan();
        plan.setOrderId(Long.valueOf(String.valueOf(body.get("orderId"))));
        plan.setCustomerName(String.valueOf(body.getOrDefault("customerName", "")));
        if (body.get("roomId") != null) {
            plan.setRoomId(Long.valueOf(String.valueOf(body.get("roomId"))));
        }
        plan.setCareMode((String) body.getOrDefault("careMode", "一对一专护"));
        int days = body.get("days") == null ? 28 : Integer.parseInt(String.valueOf(body.get("days")));
        LocalDate start = body.get("startDate") == null ? LocalDate.now()
                : LocalDate.parse(String.valueOf(body.get("startDate")));
        String deliveryType = (String) body.getOrDefault("deliveryType", "自然分娩");
        return R.ok(careService.createPlan(plan, start, days, deliveryType));
    }

    @Operation(summary = "护理计划详情（按日期分组的计划项）")
    @GetMapping("/plans/{id}")
    public R<Map<String, Object>> planDetail(@PathVariable Long id) {
        AuthGuard.requireTenant();
        return R.ok(careService.planDetail(id));
    }

    @Operation(summary = "新增护理计划项")
    @PostMapping("/plans/{id}/items")
    public R<Void> addItem(@PathVariable Long id, @RequestBody CarePlanItem request) {
        AuthGuard.requireTenant();
        careService.addItem(id, request);
        return R.ok();
    }

    @Operation(summary = "删除护理计划项")
    @DeleteMapping("/items/{itemId}")
    public R<Void> removeItem(@PathVariable Long itemId) {
        AuthGuard.requireTenant();
        careService.removeItem(itemId);
        return R.ok();
    }

    @Operation(summary = "执行护理项并记录")
    @PostMapping("/items/{itemId}/execute")
    public R<Long> execute(@PathVariable Long itemId, @RequestBody CareRecord request) {
        AuthGuard.requireTenant();
        return R.ok(careService.execute(itemId, request));
    }

    @Operation(summary = "护理记录列表")
    @GetMapping("/records")
    public R<List<CareRecord>> records(@RequestParam(required = false) Long orderId,
                                       @RequestParam(required = false) String targetType) {
        AuthGuard.requireTenant();
        return R.ok(careService.records(orderId, targetType));
    }

    @Operation(summary = "修改护理记录")
    @PutMapping("/records/{id}")
    public R<Void> updateRecord(@PathVariable Long id, @RequestBody CareRecord request) {
        AuthGuard.requireTenant();
        careService.updateRecord(id, request);
        return R.ok();
    }

    @Operation(summary = "审批超时补录记录")
    @PutMapping("/records/{id}/approve")
    public R<Void> approve(@PathVariable Long id) {
        AuthGuard.requireTenant();
        careService.approveRecord(id);
        return R.ok();
    }

    @Operation(summary = "体征趋势")
    @GetMapping("/records/trend")
    public R<List<Map<String, Object>>> trend(@RequestParam Long orderId,
                                              @RequestParam(required = false) String targetType) {
        AuthGuard.requireTenant();
        return R.ok(careService.trend(orderId, targetType));
    }

    @Operation(summary = "护理看板")
    @GetMapping("/board")
    public R<Map<String, Object>> board(@RequestParam(required = false)
                                        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        AuthGuard.requireTenant();
        return R.ok(careService.board(date));
    }

    @Operation(summary = "交接班列表")
    @GetMapping("/handovers")
    public R<List<CareHandover>> handovers(@RequestParam(required = false)
                                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        AuthGuard.requireTenant();
        return R.ok(careService.handovers(date));
    }

    @Operation(summary = "新建交接单")
    @PostMapping("/handovers")
    public R<Long> createHandover(@RequestBody CareHandover request) {
        AuthGuard.requireTenant();
        return R.ok(careService.createHandover(request));
    }

    @Operation(summary = "接班确认（签名）")
    @PostMapping("/handovers/{id}/confirm")
    public R<Void> confirmHandover(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        AuthGuard.requireTenant();
        careService.confirmHandover(id, body == null ? null : body.get("signKey"));
        return R.ok();
    }

    @Operation(summary = "交接及时率")
    @GetMapping("/handovers/stats")
    public R<Map<String, Object>> handoverStats(@RequestParam(required = false)
                                                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                @RequestParam(required = false)
                                                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        AuthGuard.requireTenant();
        return R.ok(careService.handoverStats(from, to));
    }

    @Operation(summary = "异常列表")
    @GetMapping("/abnormals")
    public R<List<CareAbnormal>> abnormals(@RequestParam(required = false) String status,
                                           @RequestParam(required = false) String urgency) {
        AuthGuard.requireTenant();
        return R.ok(careService.abnormals(status, urgency));
    }

    @Operation(summary = "上报异常")
    @PostMapping("/abnormals")
    public R<Long> report(@RequestBody CareAbnormal request) {
        AuthGuard.requireTenant();
        return R.ok(careService.reportAbnormal(request));
    }

    @Operation(summary = "处置异常")
    @PutMapping("/abnormals/{id}/handle")
    public R<Void> handle(@PathVariable Long id, @RequestBody CareAbnormal request) {
        AuthGuard.requireTenant();
        careService.handleAbnormal(id, request);
        return R.ok();
    }
}
