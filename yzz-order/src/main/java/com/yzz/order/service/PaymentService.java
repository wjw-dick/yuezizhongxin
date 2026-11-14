package com.yzz.order.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yzz.common.api.BusinessException;
import com.yzz.common.api.ErrorCode;
import com.yzz.common.tenant.TenantContext;
import com.yzz.order.dto.OrderPayRequest;
import com.yzz.order.dto.RefundRequest;
import com.yzz.order.entity.BizOrder;
import com.yzz.order.entity.BizPayment;
import com.yzz.order.entity.BizRefund;
import com.yzz.order.mapper.BizPaymentMapper;
import com.yzz.order.mapper.BizRefundMapper;
import com.yzz.order.pay.WeChatPayClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 收款、退款与支付回调（T36、T37）。
 *
 * <p>幂等口径：回调按收款单号 + 交易号去重；同一笔收款重复回调只推进一次订单状态。
 * 金额一律以分为单位与微信交互，库内保留两位小数。</p>
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final BizPaymentMapper paymentMapper;
    private final BizRefundMapper refundMapper;
    private final OrderService orderService;
    private final WeChatPayClient payClient;
    private final ObjectMapper objectMapper;

    public PaymentService(BizPaymentMapper paymentMapper, BizRefundMapper refundMapper,
                          OrderService orderService, WeChatPayClient payClient,
                          ObjectMapper objectMapper) {
        this.paymentMapper = paymentMapper;
        this.refundMapper = refundMapper;
        this.orderService = orderService;
        this.payClient = payClient;
        this.objectMapper = objectMapper;
    }

    /**
     * 发起收款。微信支付返回拉起参数；线下方式（现金/转账）当场置为已支付。
     */
    @Transactional
    public Map<String, Object> pay(Long orderId, OrderPayRequest request) {
        BizOrder order = orderService.requireOrder(orderId);
        if (List.of("已取消", "已退订", "已完成").contains(order.getStatus())) {
            throw BusinessException.of(ErrorCode.CONFLICT, "当前订单状态不可收款：" + order.getStatus());
        }
        String payType = request.payType() == null ? "定金" : request.payType();
        BigDecimal amount = request.amount() == null ? defaultAmount(order, payType) : request.amount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw BusinessException.of(ErrorCode.BAD_REQUEST, "收款金额必须大于 0");
        }
        String method = request.method() == null ? "微信支付" : request.method();
        BizPayment payment = orderService.createPaymentRecord(orderId, payType, method, amount);
        payment.setRemark(request.remark());
        paymentMapper.updateById(payment);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("paymentId", payment.getId());
        result.put("paymentNo", payment.getPaymentNo());
        result.put("amount", amount);
        result.put("method", method);

        if ("微信支付".equals(method)) {
            Map<String, Object> prepay = payClient.prepay(
                    payment.getPaymentNo(), order.getCustomerName() + " " + payType,
                    amount.multiply(BigDecimal.valueOf(100)).longValue(), null);
            result.put("prepay", prepay);
            return result;
        }

        markSuccess(payment, "OFFLINE-" + payment.getPaymentNo(), LocalDateTime.now());
        result.put("paid", true);
        return result;
    }

    /** 模拟支付成功：仅在未配置真实商户参数时可用，用于本地联调与验收。 */
    @Transactional
    public void mockSuccess(Long paymentId) {
        if (payClient.realMode()) {
            throw BusinessException.of(ErrorCode.FORBIDDEN, "已启用真实微信支付，禁止模拟支付");
        }
        BizPayment payment = requirePayment(paymentId);
        markSuccess(payment, "MOCK-" + payment.getPaymentNo(), LocalDateTime.now());
    }

    /** 主动查单并修复本地状态。 */
    @Transactional
    public String query(Long paymentId) {
        BizPayment payment = requirePayment(paymentId);
        if ("已支付".equals(payment.getStatus())) {
            return "SUCCESS";
        }
        String state = payClient.query(payment.getPaymentNo());
        if ("SUCCESS".equals(state)) {
            markSuccess(payment, "QUERY-" + payment.getPaymentNo(), LocalDateTime.now());
        }
        return state;
    }

    /**
     * 支付回调。返回给微信的报文由 Controller 组装。
     */
    @Transactional
    public void handleNotify(Map<String, String> headers, String body) {
        if (!payClient.verifyNotify(headers, body)) {
            throw BusinessException.of(ErrorCode.FORBIDDEN, "支付回调验签失败");
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode resource = root.path("resource");
            JsonNode data = resource.has("ciphertext") ? payClient.decryptResource(resource) : root;
            String paymentNo = data.path("out_trade_no").asText();
            String tradeNo = data.path("transaction_id").asText();
            String state = data.path("trade_state").asText("SUCCESS");
            if (paymentNo.isBlank()) {
                log.warn("支付回调缺少收款单号，忽略：{}", body);
                return;
            }
            BizPayment payment = paymentMapper.selectOne(Wrappers.<BizPayment>lambdaQuery()
                    .eq(BizPayment::getPaymentNo, paymentNo).last("limit 1"));
            if (payment == null) {
                log.warn("支付回调找不到收款单 {}", paymentNo);
                return;
            }
            if ("SUCCESS".equals(state)) {
                markSuccess(payment, tradeNo, LocalDateTime.now());
            } else {
                payment.setStatus("支付异常");
                payment.setRemark("回调状态：" + state);
                paymentMapper.updateById(payment);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("支付回调处理失败 body={}", body, e);
            throw BusinessException.of(ErrorCode.INTERNAL_ERROR, "支付回调处理失败");
        }
    }

    @Transactional
    public BizRefund applyRefund(Long orderId, RefundRequest request) {
        BizOrder order = orderService.requireOrder(orderId);
        BigDecimal paid = order.getPaidAmount() == null ? BigDecimal.ZERO : order.getPaidAmount();
        BigDecimal refunded = refundMapper.selectList(Wrappers.<BizRefund>lambdaQuery()
                        .eq(BizRefund::getOrderId, orderId)
                        .in(BizRefund::getStatus, List.of("已通过", "已退款")))
                .stream().map(BizRefund::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal available = paid.subtract(refunded);
        BigDecimal amount = request.amount() == null ? available : request.amount();
        if (amount.compareTo(BigDecimal.ZERO) <= 0 || amount.compareTo(available) > 0) {
            throw BusinessException.of(ErrorCode.BAD_REQUEST,
                    "退款金额需大于 0 且不超过可退金额 " + available + " 元");
        }
        if (request.reasonCode() == null || request.reasonCode().isBlank()) {
            throw BusinessException.of(ErrorCode.BAD_REQUEST, "退款原因必填");
        }
        BizRefund refund = new BizRefund();
        refund.setTenantId(TenantContext.requireTenantId());
        refund.setOrderId(orderId);
        refund.setRefundNo("TK" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%03d", (int) (Math.random() * 1000)));
        refund.setAmount(amount);
        refund.setPenaltyAmount(request.penaltyAmount() == null ? BigDecimal.ZERO : request.penaltyAmount());
        refund.setReasonCode(request.reasonCode());
        refund.setReason(request.reason());
        refund.setStatus("待审核");
        refundMapper.insert(refund);
        return refund;
    }

    @Transactional
    public void auditRefund(Long refundId, RefundRequest request) {
        BizRefund refund = refundMapper.selectById(refundId);
        if (refund == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "退款单不存在");
        }
        if (!"待审核".equals(refund.getStatus())) {
            throw BusinessException.of(ErrorCode.CONFLICT, "该退款单已处理");
        }
        refund.setAuditBy(TenantContext.getUserId());
        refund.setAuditTime(LocalDateTime.now());
        if (Boolean.FALSE.equals(request.approved())) {
            refund.setStatus("已驳回");
            refund.setReason(request.auditRemark() == null ? refund.getReason() : request.auditRemark());
            refundMapper.updateById(refund);
            return;
        }
        refund.setStatus("已退款");
        refundMapper.updateById(refund);
        BizOrder order = orderService.requireOrder(refund.getOrderId());
        BigDecimal refundedTotal = refundMapper.selectList(Wrappers.<BizRefund>lambdaQuery()
                        .eq(BizRefund::getOrderId, order.getId())
                        .eq(BizRefund::getStatus, "已退款"))
                .stream().map(BizRefund::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (refundedTotal.compareTo(order.getPaidAmount()) >= 0) {
            orderService.markRefunded(order.getId());
        }
        log.info("退款完成 refundNo={} amount={} orderNo={}", refund.getRefundNo(),
                refund.getAmount(), order.getOrderNo());
    }

    public List<BizPayment> pendingPayments() {
        return paymentMapper.selectList(Wrappers.<BizPayment>lambdaQuery()
                .in(BizPayment::getStatus, List.of("待支付", "支付异常"))
                .eq(BizPayment::getMethod, "微信支付")
                .orderByAsc(BizPayment::getId).last("limit 200"));
    }

    /** 退款单列表，供退订审核列表使用。 */
    public List<BizRefund> refundList(String status) {
        return refundMapper.selectList(Wrappers.<BizRefund>lambdaQuery()
                .eq(status != null && !status.isBlank(), BizRefund::getStatus, status)
                .orderByDesc(BizRefund::getId).last("limit 200"));
    }

    public BizPayment requirePayment(Long id) {
        BizPayment payment = paymentMapper.selectById(id);
        if (payment == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "收款单不存在");
        }
        return payment;
    }

    private void markSuccess(BizPayment payment, String tradeNo, LocalDateTime paidTime) {
        if ("已支付".equals(payment.getStatus())) {
            return;
        }
        payment.setStatus("已支付");
        payment.setTradeNo(tradeNo);
        payment.setPaidTime(paidTime);
        paymentMapper.updateById(payment);
        orderService.markPaid(payment.getOrderId(), payment.getAmount(), payment.getPayType());
        log.info("收款成功 paymentNo={} orderId={} amount={} type={}", payment.getPaymentNo(),
                payment.getOrderId(), payment.getAmount(), payment.getPayType());
    }

    private BigDecimal defaultAmount(BizOrder order, String payType) {
        return switch (payType) {
            case "定金" -> order.getDepositAmount();
            case "尾款" -> order.getTailAmount().subtract(
                    order.getPaidAmount().subtract(order.getDepositAmount()).max(BigDecimal.ZERO))
                    .max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
            case "押金" -> order.getBondAmount();
            default -> BigDecimal.ZERO;
        };
    }
}
