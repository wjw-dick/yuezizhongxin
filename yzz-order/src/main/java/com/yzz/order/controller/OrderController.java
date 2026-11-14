package com.yzz.order.controller;

import com.yzz.common.api.PageResult;
import com.yzz.common.api.R;
import com.yzz.common.tenant.AuthGuard;
import com.yzz.order.dto.OrderChangeRequest;
import com.yzz.order.dto.OrderCreateRequest;
import com.yzz.order.dto.OrderPayRequest;
import com.yzz.order.dto.RefundRequest;
import com.yzz.order.entity.BizOrder;
import com.yzz.order.entity.BizPayment;
import com.yzz.order.entity.BizRefund;
import com.yzz.order.service.OrderService;
import com.yzz.order.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 订单、收款与退订（T36、T37）。
 */
@Tag(name = "租户-订单与收款")
@RestController
@RequestMapping("/api/tenant/orders")
public class OrderController {

    private final OrderService orderService;
    private final PaymentService paymentService;

    public OrderController(OrderService orderService, PaymentService paymentService) {
        this.orderService = orderService;
        this.paymentService = paymentService;
    }

    @Operation(summary = "订单分页列表")
    @GetMapping
    public R<PageResult<BizOrder>> list(@RequestParam(required = false) String status,
                                        @RequestParam(required = false) String keyword,
                                        @RequestParam(required = false)
                                        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                        @RequestParam(required = false)
                                        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                        @RequestParam(defaultValue = "1") int pageNum,
                                        @RequestParam(defaultValue = "10") int pageSize) {
        AuthGuard.requireTenant();
        return R.ok(orderService.list(status, keyword, from, to, pageNum, Math.min(pageSize, 100)));
    }

    @Operation(summary = "创建订单（代客下单）")
    @PostMapping
    public R<Map<String, Object>> create(@RequestBody OrderCreateRequest request) {
        AuthGuard.requireTenant();
        return R.ok(orderService.create(request));
    }

    @Operation(summary = "订单详情（含明细、收款、退款、变更、合同）")
    @GetMapping("/{id}")
    public R<Map<String, Object>> detail(@PathVariable Long id) {
        AuthGuard.requireTenant();
        return R.ok(orderService.detail(id));
    }

    @Operation(summary = "取消待支付订单")
    @PostMapping("/{id}/cancel")
    public R<Void> cancel(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        AuthGuard.requireTenant();
        orderService.cancel(id, body == null ? null : body.get("reason"));
        return R.ok();
    }

    @Operation(summary = "订单变更（换房/延期/改套餐）")
    @PostMapping("/{id}/change")
    public R<Map<String, Object>> change(@PathVariable Long id, @RequestBody OrderChangeRequest request) {
        AuthGuard.requireTenant();
        return R.ok(orderService.change(id, request));
    }

    @Operation(summary = "上传合同附件")
    @PostMapping("/{id}/contracts")
    public R<Void> addContract(@PathVariable Long id, @RequestBody Map<String, String> body) {
        AuthGuard.requireTenant();
        orderService.addContract(id, body.get("fileKey"), body.get("fileName"));
        return R.ok();
    }

    @Operation(summary = "发起收款")
    @PostMapping("/{id}/pay")
    public R<Map<String, Object>> pay(@PathVariable Long id, @RequestBody OrderPayRequest request) {
        AuthGuard.requireTenant();
        return R.ok(paymentService.pay(id, request));
    }

    @Operation(summary = "模拟支付成功（仅本地未配置商户参数时可用）")
    @PostMapping("/payments/{paymentId}/mock-success")
    public R<Void> mockSuccess(@PathVariable Long paymentId) {
        AuthGuard.requireTenant();
        paymentService.mockSuccess(paymentId);
        return R.ok();
    }

    @Operation(summary = "主动查单")
    @PostMapping("/payments/{paymentId}/query")
    public R<String> query(@PathVariable Long paymentId) {
        AuthGuard.requireTenant();
        return R.ok(paymentService.query(paymentId));
    }

    @Operation(summary = "待对账收款单")
    @GetMapping("/payments/pending")
    public R<List<BizPayment>> pending() {
        AuthGuard.requireTenant();
        return R.ok(paymentService.pendingPayments());
    }

    @Operation(summary = "退订申请")
    @PostMapping("/{id}/refunds")
    public R<BizRefund> applyRefund(@PathVariable Long id, @RequestBody RefundRequest request) {
        AuthGuard.requireTenant();
        return R.ok(paymentService.applyRefund(id, request));
    }

    @Operation(summary = "退款单列表")
    @GetMapping("/refunds")
    public R<List<BizRefund>> refunds(@RequestParam(required = false) String status) {
        AuthGuard.requireTenant();
        return R.ok(paymentService.refundList(status));
    }

    @Operation(summary = "退订审核")
    @PostMapping("/refunds/{refundId}/audit")
    public R<Void> auditRefund(@PathVariable Long refundId, @RequestBody RefundRequest request) {
        AuthGuard.requireTenant();
        paymentService.auditRefund(refundId, request);
        return R.ok();
    }
}
