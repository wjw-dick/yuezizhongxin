package com.yzz.report.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yzz.care.entity.CareAbnormal;
import com.yzz.care.entity.CareHandover;
import com.yzz.care.entity.CarePlanItem;
import com.yzz.care.entity.CareRecord;
import com.yzz.care.mapper.CareAbnormalMapper;
import com.yzz.care.mapper.CareHandoverMapper;
import com.yzz.care.mapper.CarePlanItemMapper;
import com.yzz.care.mapper.CareRecordMapper;
import com.yzz.crm.entity.CrmLead;
import com.yzz.crm.mapper.CrmLeadMapper;
import com.yzz.meal.entity.MealOrder;
import com.yzz.meal.mapper.MealOrderMapper;
import com.yzz.order.entity.BizOrder;
import com.yzz.order.entity.BizPayment;
import com.yzz.order.entity.BizRefund;
import com.yzz.order.mapper.BizOrderMapper;
import com.yzz.order.mapper.BizPaymentMapper;
import com.yzz.order.mapper.BizRefundMapper;
import com.yzz.room.entity.Room;
import com.yzz.room.mapper.RoomMapper;
import com.yzz.staff.service.StaffService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 经营、护理、人力与餐食报表（T49）。
 *
 * <p>口径说明：
 * <ul>
 *   <li>入住率 = 在住房间夜 / 可售房间夜；月报按订单与月份的重叠天数折算。</li>
 *   <li>客单价 = 期间实收 / 期间新签订单数。</li>
 *   <li>转化率 = 期间转订单的线索数 / 期间新增线索数。</li>
 *   <li>退订率 = 期间退款单数 / 期间新签订单数。</li>
 * </ul>
 */
@Service
public class ReportService {

    private final BizOrderMapper orderMapper;
    private final BizPaymentMapper paymentMapper;
    private final BizRefundMapper refundMapper;
    private final RoomMapper roomMapper;
    private final CrmLeadMapper leadMapper;
    private final CarePlanItemMapper planItemMapper;
    private final CareRecordMapper careRecordMapper;
    private final CareAbnormalMapper abnormalMapper;
    private final CareHandoverMapper handoverMapper;
    private final MealOrderMapper mealOrderMapper;
    private final StaffService staffService;

    public ReportService(BizOrderMapper orderMapper, BizPaymentMapper paymentMapper,
                         BizRefundMapper refundMapper, RoomMapper roomMapper,
                         CrmLeadMapper leadMapper, CarePlanItemMapper planItemMapper,
                         CareRecordMapper careRecordMapper, CareAbnormalMapper abnormalMapper,
                         CareHandoverMapper handoverMapper, MealOrderMapper mealOrderMapper,
                         StaffService staffService) {
        this.orderMapper = orderMapper;
        this.paymentMapper = paymentMapper;
        this.refundMapper = refundMapper;
        this.roomMapper = roomMapper;
        this.leadMapper = leadMapper;
        this.planItemMapper = planItemMapper;
        this.careRecordMapper = careRecordMapper;
        this.abnormalMapper = abnormalMapper;
        this.handoverMapper = handoverMapper;
        this.mealOrderMapper = mealOrderMapper;
        this.staffService = staffService;
    }

    public Map<String, Object> daily(LocalDate date) {
        LocalDate day = date == null ? LocalDate.now() : date;
        LocalDateTime start = day.atStartOfDay();
        LocalDateTime end = day.plusDays(1).atStartOfDay();
        List<Room> rooms = roomMapper.selectList(Wrappers.<Room>lambdaQuery());
        long roomCount = rooms.size();
        long living = rooms.stream().filter(r -> "OCCUPIED".equals(r.getStatus())).count();
        long checkins = orderMapper.selectCount(Wrappers.<BizOrder>lambdaQuery()
                .eq(BizOrder::getCheckinDate, day));
        long checkouts = orderMapper.selectCount(Wrappers.<BizOrder>lambdaQuery()
                .eq(BizOrder::getCheckoutDate, day));
        long orders = orderMapper.selectCount(Wrappers.<BizOrder>lambdaQuery()
                .ge(BizOrder::getCreatedTime, start).lt(BizOrder::getCreatedTime, end));
        List<BizPayment> payments = paymentMapper.selectList(Wrappers.<BizPayment>lambdaQuery()
                .eq(BizPayment::getStatus, "已支付")
                .ge(BizPayment::getPaidTime, start).lt(BizPayment::getPaidTime, end));
        BigDecimal revenue = payments.stream().map(BizPayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<BizRefund> refunds = refundMapper.selectList(Wrappers.<BizRefund>lambdaQuery()
                .eq(BizRefund::getStatus, "已退款")
                .ge(BizRefund::getCreatedTime, start).lt(BizRefund::getCreatedTime, end));
        BigDecimal refundAmount = refunds.stream().map(BizRefund::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long leads = leadMapper.selectCount(Wrappers.<CrmLead>lambdaQuery()
                .ge(CrmLead::getCreatedTime, start).lt(CrmLead::getCreatedTime, end));
        long converted = orderMapper.selectCount(Wrappers.<BizOrder>lambdaQuery()
                .isNotNull(BizOrder::getLeadId)
                .ge(BizOrder::getCreatedTime, start).lt(BizOrder::getCreatedTime, end));
        List<BizOrder> dayOrders = orderMapper.selectList(Wrappers.<BizOrder>lambdaQuery()
                .ge(BizOrder::getCreatedTime, start).lt(BizOrder::getCreatedTime, end));
        BigDecimal extra = dayOrders.stream()
                .map(o -> nvl(o.getExtraAmount()).add(nvl(o.getProductAmount())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal total = dayOrders.stream().map(o -> nvl(o.getTotalAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("date", day.toString());
        result.put("roomCount", roomCount);
        result.put("occupied", living);
        result.put("occupancyRate", rate(living, roomCount));
        result.put("checkins", checkins);
        result.put("checkouts", checkouts);
        result.put("newOrders", orders);
        result.put("revenue", scale(revenue));
        result.put("avgOrderAmount", orders == 0 ? BigDecimal.ZERO : revenue.divide(
                BigDecimal.valueOf(orders), 2, RoundingMode.HALF_UP));
        result.put("refundCount", refunds.size());
        result.put("refundAmount", scale(refundAmount));
        result.put("refundRate", rate(refunds.size(), Math.max(orders, 1)));
        result.put("newLeads", leads);
        result.put("convertedLeads", converted);
        result.put("conversionRate", rate(converted, Math.max(leads, 1)));
        result.put("extraRevenueShare", total.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
                : extra.multiply(BigDecimal.valueOf(100)).divide(total, 1, RoundingMode.HALF_UP));
        result.put("channelMix", channelMix(dayOrders));
        return result;
    }

    public Map<String, Object> monthly(YearMonth month) {
        YearMonth target = month == null ? YearMonth.now() : month;
        LocalDate from = target.atDay(1);
        LocalDate to = target.atEndOfMonth();
        List<Room> rooms = roomMapper.selectList(Wrappers.<Room>lambdaQuery());
        int roomCount = rooms.size();
        int days = target.lengthOfMonth();
        List<BizOrder> orders = orderMapper.selectList(Wrappers.<BizOrder>lambdaQuery()
                .le(BizOrder::getCheckinDate, to)
                .ge(BizOrder::getCheckoutDate, from)
                .ne(BizOrder::getStatus, "已取消"));
        long roomNights = 0;
        for (BizOrder order : orders) {
            LocalDate begin = order.getCheckinDate().isBefore(from) ? from : order.getCheckinDate();
            LocalDate end = order.getCheckoutDate().isAfter(to) ? to.plusDays(1) : order.getCheckoutDate();
            long nights = Math.max(0, end.toEpochDay() - begin.toEpochDay());
            roomNights += nights;
        }
        long capacity = (long) roomCount * days;
        List<BizPayment> payments = paymentMapper.selectList(Wrappers.<BizPayment>lambdaQuery()
                .eq(BizPayment::getStatus, "已支付")
                .ge(BizPayment::getPaidTime, from.atStartOfDay())
                .lt(BizPayment::getPaidTime, to.plusDays(1).atStartOfDay()));
        BigDecimal revenue = payments.stream().map(BizPayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long newOrders = orderMapper.selectCount(Wrappers.<BizOrder>lambdaQuery()
                .ge(BizOrder::getCreatedTime, from.atStartOfDay())
                .lt(BizOrder::getCreatedTime, to.plusDays(1).atStartOfDay()));
        long refunds = refundMapper.selectCount(Wrappers.<BizRefund>lambdaQuery()
                .eq(BizRefund::getStatus, "已退款")
                .ge(BizRefund::getCreatedTime, from.atStartOfDay())
                .lt(BizRefund::getCreatedTime, to.plusDays(1).atStartOfDay()));
        long leads = leadMapper.selectCount(Wrappers.<CrmLead>lambdaQuery()
                .ge(CrmLead::getCreatedTime, from.atStartOfDay())
                .lt(CrmLead::getCreatedTime, to.plusDays(1).atStartOfDay()));
        long converted = orderMapper.selectCount(Wrappers.<BizOrder>lambdaQuery()
                .isNotNull(BizOrder::getLeadId)
                .ge(BizOrder::getCreatedTime, from.atStartOfDay())
                .lt(BizOrder::getCreatedTime, to.plusDays(1).atStartOfDay()));
        BigDecimal extra = orders.stream()
                .map(o -> nvl(o.getExtraAmount()).add(nvl(o.getProductAmount())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal total = orders.stream().map(o -> nvl(o.getTotalAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("month", target.toString());
        result.put("capacityRoomNights", capacity);
        result.put("roomNights", roomNights);
        result.put("occupancyRate", rate(roomNights, capacity));
        result.put("turnover", roomNights == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(capacity).divide(BigDecimal.valueOf(roomNights), 2, RoundingMode.HALF_UP));
        result.put("newOrders", newOrders);
        result.put("revenue", scale(revenue));
        result.put("avgOrderAmount", newOrders == 0 ? BigDecimal.ZERO
                : revenue.divide(BigDecimal.valueOf(newOrders), 2, RoundingMode.HALF_UP));
        result.put("refundRate", rate(refunds, Math.max(newOrders, 1)));
        result.put("conversionRate", rate(converted, Math.max(leads, 1)));
        result.put("extraRevenueShare", total.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
                : extra.multiply(BigDecimal.valueOf(100)).divide(total, 1, RoundingMode.HALF_UP));
        result.put("channelMix", channelMix(orders));
        result.put("dailyTrend", trend(from, to));
        return result;
    }

    public Map<String, Object> care(LocalDate from, LocalDate to) {
        LocalDate start = from == null ? LocalDate.now().minusDays(29) : from;
        LocalDate end = to == null ? LocalDate.now() : to;
        List<CarePlanItem> items = planItemMapper.selectList(Wrappers.<CarePlanItem>lambdaQuery()
                .between(CarePlanItem::getPlanDate, start, end));
        long done = items.stream().filter(i -> "已完成".equals(i.getStatus())).count();
        long records = careRecordMapper.selectCount(Wrappers.<CareRecord>lambdaQuery()
                .ge(CareRecord::getSubmitTime, start.atStartOfDay())
                .lt(CareRecord::getSubmitTime, end.plusDays(1).atStartOfDay()));
        List<CareAbnormal> abnormals = abnormalMapper.selectList(Wrappers.<CareAbnormal>lambdaQuery()
                .ge(CareAbnormal::getCreatedTime, start.atStartOfDay())
                .lt(CareAbnormal::getCreatedTime, end.plusDays(1).atStartOfDay()));
        long closed = abnormals.stream().filter(a -> "已闭环".equals(a.getStatus())).count();
        List<CareHandover> handovers = handoverMapper.selectList(Wrappers.<CareHandover>lambdaQuery()
                .between(CareHandover::getShiftDate, start, end));
        long confirmed = handovers.stream().filter(h -> "已确认".equals(h.getStatus())).count();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("from", start.toString());
        result.put("to", end.toString());
        result.put("planItems", items.size());
        result.put("finishedItems", done);
        result.put("finishRate", rate(done, Math.max(items.size(), 1)));
        result.put("records", records);
        result.put("abnormals", abnormals.size());
        result.put("abnormalClosed", closed);
        result.put("abnormalCloseRate", rate(closed, Math.max(abnormals.size(), 1)));
        result.put("urgentAbnormals", abnormals.stream().filter(a -> "特急".equals(a.getUrgency())).count());
        result.put("handoverRate", rate(confirmed, Math.max(handovers.size(), 1)));
        return result;
    }

    public Map<String, Object> meal(LocalDate from, LocalDate to) {
        LocalDate start = from == null ? LocalDate.now().minusDays(6) : from;
        LocalDate end = to == null ? LocalDate.now() : to;
        List<MealOrder> rows = mealOrderMapper.selectList(Wrappers.<MealOrder>lambdaQuery()
                .between(MealOrder::getMealDate, start, end));
        long checked = rows.stream().filter(r -> Integer.valueOf(1).equals(r.getChecked())).count();
        long conflicts = rows.stream().filter(r -> "忌口冲突".equals(r.getSpecialTag())).count();
        BigDecimal addOn = rows.stream().map(r -> nvl(r.getExtraCharge()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("from", start.toString());
        result.put("to", end.toString());
        result.put("meals", rows.size());
        result.put("checked", checked);
        result.put("checkRate", rate(checked, Math.max(rows.size(), 1)));
        result.put("conflicts", conflicts);
        result.put("addOnCharge", scale(addOn));
        return result;
    }

    public List<Map<String, Object>> staff(LocalDate from, LocalDate to) {
        LocalDate start = from == null ? LocalDate.now().withDayOfMonth(1) : from;
        LocalDate end = to == null ? LocalDate.now() : to;
        return staffService.workload(start, end);
    }

    private List<Map<String, Object>> trend(LocalDate from, LocalDate to) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("date", d.toString());
            BigDecimal revenue = paymentMapper.selectList(Wrappers.<BizPayment>lambdaQuery()
                            .eq(BizPayment::getStatus, "已支付")
                            .ge(BizPayment::getPaidTime, d.atStartOfDay())
                            .lt(BizPayment::getPaidTime, d.plusDays(1).atStartOfDay()))
                    .stream().map(BizPayment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            long orders = orderMapper.selectCount(Wrappers.<BizOrder>lambdaQuery()
                    .ge(BizOrder::getCreatedTime, d.atStartOfDay())
                    .lt(BizOrder::getCreatedTime, d.plusDays(1).atStartOfDay()));
            row.put("revenue", scale(revenue));
            row.put("orders", orders);
            rows.add(row);
        }
        rows.sort(Comparator.comparing(m -> String.valueOf(m.get("date"))));
        return rows;
    }

    private List<Map<String, Object>> channelMix(List<BizOrder> orders) {
        Map<String, Integer> mix = new LinkedHashMap<>();
        for (BizOrder order : orders) {
            String key = order.getSource() == null ? "未标注" : order.getSource();
            mix.merge(key, 1, Integer::sum);
        }
        int total = orders.size();
        List<Map<String, Object>> result = new ArrayList<>();
        mix.forEach((source, count) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("source", source);
            row.put("count", count);
            row.put("ratio", rate(count, Math.max(total, 1)));
            result.add(row);
        });
        result.sort((a, b) -> (int) b.get("count") - (int) a.get("count"));
        return result;
    }

    private BigDecimal rate(long value, long total) {
        if (total <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(value).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP);
    }

    private BigDecimal scale(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /**
     * 后台首页工作台（T54）：今日概览 + 提醒区。
     */
    public Map<String, Object> workbench() {
        LocalDate today = LocalDate.now();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("today", daily(today));

        List<Map<String, Object>> reminders = new ArrayList<>();
        long pendingPay = orderMapper.selectCount(Wrappers.<BizOrder>lambdaQuery()
                .eq(BizOrder::getStatus, "待支付"));
        long pendingCheckin = orderMapper.selectCount(Wrappers.<BizOrder>lambdaQuery()
                .eq(BizOrder::getStatus, "待入住"));
        long living = orderMapper.selectCount(Wrappers.<BizOrder>lambdaQuery()
                .eq(BizOrder::getStatus, "在住"));
        long pendingSettle = orderMapper.selectCount(Wrappers.<BizOrder>lambdaQuery()
                .eq(BizOrder::getStatus, "待结算"));
        reminders.add(reminder("订单", "待支付订单", pendingPay, "/orders?status=待支付"));
        reminders.add(reminder("订单", "今日待入住", pendingCheckin, "/orders?status=待入住"));
        reminders.add(reminder("订单", "在住客户", living, "/stay"));
        reminders.add(reminder("订单", "待结算", pendingSettle, "/stay?tab=checkout"));

        long abnormal = abnormalMapper.selectCount(Wrappers.<CareAbnormal>lambdaQuery()
                .ne(CareAbnormal::getStatus, "已闭环"));
        reminders.add(reminder("护理", "未闭环异常", abnormal, "/care?tab=abnormal"));
        long todayItems = planItemMapper.selectCount(Wrappers.<CarePlanItem>lambdaQuery()
                .eq(CarePlanItem::getPlanDate, today).eq(CarePlanItem::getStatus, "待执行"));
        reminders.add(reminder("护理", "今日待执行护理项", todayItems, "/care"));

        long pendingRefund = refundMapper.selectCount(Wrappers.<BizRefund>lambdaQuery()
                .eq(BizRefund::getStatus, "待审核"));
        reminders.add(reminder("财务", "待审核退订", pendingRefund, "/orders?tab=refund"));

        long overdueLeads = leadMapper.selectCount(Wrappers.<CrmLead>lambdaQuery()
                .isNotNull(CrmLead::getNextFollowTime)
                .lt(CrmLead::getNextFollowTime, LocalDateTime.now())
                .notIn(CrmLead::getStage, List.of("已签约", "已流失")));
        reminders.add(reminder("CRM", "跟进超时线索", overdueLeads, "/crm"));

        long expiringCerts = staffService.expiringCerts(60).size();
        reminders.add(reminder("人力", "证件 60 天内到期", expiringCerts, "/staff"));

        result.put("reminders", reminders);
        result.put("pendingTotal", reminders.stream()
                .mapToLong(r -> ((Number) r.get("count")).longValue()).sum());
        return result;
    }

    private Map<String, Object> reminder(String module, String title, long count, String link) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("module", module);
        item.put("title", title);
        item.put("count", count);
        item.put("link", link);
        return item;
    }
}
