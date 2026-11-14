package com.yzz.order.controller;

import com.yzz.common.api.R;
import com.yzz.common.guard.Idempotent;
import com.yzz.order.dto.OrderCreateRequest;
import com.yzz.order.dto.OrderPayRequest;
import com.yzz.order.service.OrderService;
import com.yzz.order.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 小程序/H5/官网下单与支付（T74、T75、T86）。
 *
 * <p>开放接口不要求登录态，但必须有租户上下文（X-Tenant-Code 或子域名）；
 * 订单查询需订单号 + 手机号双因子，避免遍历订单号拖库。</p>
 */
@Tag(name = "开放接口-下单支付")
@RestController
@RequestMapping("/api/open")
public class OpenOrderController {

    private final OrderService orderService;
    private final PaymentService paymentService;

    public OpenOrderController(OrderService orderService, PaymentService paymentService) {
        this.orderService = orderService;
        this.paymentService = paymentService;
    }

    @Operation(summary = "创建订单")
    @Idempotent(ttlSeconds = 15)
    @PostMapping("/orders")
    public R<Map<String, Object>> create(@RequestBody OrderCreateRequest request) {
        return R.ok(orderService.create(request));
    }

    @Operation(summary = "订单轻量查询")
    @GetMapping("/orders/{orderNo}/query")
    public R<Map<String, Object>> query(@PathVariable String orderNo, @RequestParam String phone) {
        return R.ok(orderService.queryForCustomer(orderNo, phone));
    }

    @Operation(summary = "发起支付")
    @PostMapping("/orders/{id}/pay")
    public R<Map<String, Object>> pay(@PathVariable Long id, @RequestBody OrderPayRequest request) {
        return R.ok(paymentService.pay(id, request));
    }

    /**
     * 微信支付回调。微信要求返回 200 且 body 为 {"code":"SUCCESS"}，否则会重试 8 次。
     */
    @Operation(summary = "微信支付回调")
    @PostMapping("/pay/notify")
    public Map<String, String> notify(@RequestBody String body, HttpServletRequest request) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("wechatpay-timestamp", request.getHeader("Wechatpay-Timestamp"));
        headers.put("wechatpay-nonce", request.getHeader("Wechatpay-Nonce"));
        headers.put("wechatpay-signature", request.getHeader("Wechatpay-Signature"));
        try {
            paymentService.handleNotify(headers, body);
            return Map.of("code", "SUCCESS", "message", "成功");
        } catch (Exception e) {
            return Map.of("code", "FAIL", "message", e.getMessage() == null ? "处理失败" : e.getMessage());
        }
    }
}
