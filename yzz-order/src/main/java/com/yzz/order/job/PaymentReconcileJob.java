package com.yzz.order.job;

import com.yzz.order.entity.BizPayment;
import com.yzz.order.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 支付对账任务（T37）。
 *
 * <p>每 10 分钟扫描「待支付/支付异常」的微信收款单，主动查单修复状态；
 * 单笔最多重试 6 次（每次失败计数写日志），仍异常则留待人工在支付异常待办处理。</p>
 */
@Component
public class PaymentReconcileJob {

    private static final Logger log = LoggerFactory.getLogger(PaymentReconcileJob.class);
    private final PaymentService paymentService;

    public PaymentReconcileJob(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Scheduled(fixedDelay = 600_000, initialDelay = 120_000)
    public void reconcile() {
        List<BizPayment> pending = paymentService.pendingPayments();
        if (pending.isEmpty()) {
            return;
        }
        int repaired = 0;
        for (BizPayment payment : pending) {
            for (int attempt = 1; attempt <= 6; attempt++) {
                try {
                    String state = paymentService.query(payment.getId());
                    if ("SUCCESS".equals(state)) {
                        repaired++;
                        break;
                    }
                    if ("NOTPAY".equals(state) || "CLOSED".equals(state)) {
                        break;
                    }
                } catch (Exception e) {
                    log.warn("对账查单失败 paymentNo={} attempt={} err={}",
                            payment.getPaymentNo(), attempt, e.getMessage());
                }
            }
        }
        log.info("支付对账完成 待处理={} 本次修复={}", pending.size(), repaired);
    }
}
