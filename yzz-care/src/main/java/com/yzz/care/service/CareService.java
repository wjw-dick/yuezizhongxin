package com.yzz.care.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yzz.care.entity.CareAbnormal;
import com.yzz.care.entity.CareHandover;
import com.yzz.care.entity.CarePlan;
import com.yzz.care.entity.CarePlanItem;
import com.yzz.care.entity.CareRecord;
import com.yzz.care.mapper.CareAbnormalMapper;
import com.yzz.care.mapper.CareHandoverMapper;
import com.yzz.care.mapper.CarePlanItemMapper;
import com.yzz.care.mapper.CarePlanMapper;
import com.yzz.care.mapper.CareRecordMapper;
import com.yzz.common.api.BusinessException;
import com.yzz.common.api.ErrorCode;
import com.yzz.common.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 护理计划、执行记录、交接班与异常（T40–T43）。
 *
 * <p>记录时间一律取服务端时间；计划日期超过 24 小时才补录的必须填原因并进入待审批，
 * 审批通过前不参与看板完成率统计。</p>
 */
@Service
public class CareService {

    private static final Logger log = LoggerFactory.getLogger(CareService.class);

    private final CarePlanMapper planMapper;
    private final CarePlanItemMapper itemMapper;
    private final CareRecordMapper recordMapper;
    private final CareHandoverMapper handoverMapper;
    private final CareAbnormalMapper abnormalMapper;

    public CareService(CarePlanMapper planMapper, CarePlanItemMapper itemMapper,
                       CareRecordMapper recordMapper, CareHandoverMapper handoverMapper,
                       CareAbnormalMapper abnormalMapper) {
        this.planMapper = planMapper;
        this.itemMapper = itemMapper;
        this.recordMapper = recordMapper;
        this.handoverMapper = handoverMapper;
        this.abnormalMapper = abnormalMapper;
    }

    /**
     * 默认护理模板：按分娩方式区分伤口护理频次，按对象分产妇与新生儿。
     */
    static List<Template> templateOf(String deliveryType) {
        List<Template> list = new ArrayList<>();
        list.add(new Template("产妇", "体温测量", "08:00"));
        list.add(new Template("产妇", "恶露观察", "09:00"));
        list.add(new Template("产妇", "乳房护理", "10:00"));
        list.add(new Template("产妇", "子宫复旧", "10:30"));
        if (deliveryType != null && deliveryType.contains("剖")) {
            list.add(new Template("产妇", "剖宫产伤口护理", "11:00"));
            list.add(new Template("产妇", "剖宫产伤口护理", "17:00"));
        } else {
            list.add(new Template("产妇", "会阴伤口护理", "11:00"));
        }
        list.add(new Template("新生儿", "体温测量", "08:30"));
        list.add(new Template("新生儿", "黄疸检测", "09:30"));
        list.add(new Template("新生儿", "脐部护理", "10:30"));
        list.add(new Template("新生儿", "沐浴抚触", "15:00"));
        list.add(new Template("新生儿", "喂养记录", "20:00"));
        return list;
    }

    /**
     * 生成护理计划与每日计划项。
     */
    @Transactional
    public Long createPlan(CarePlan plan, LocalDate startDate, int days, String deliveryType) {
        plan.setTenantId(TenantContext.requireTenantId());
        plan.setTemplateCode(deliveryType != null && deliveryType.contains("剖")
                ? "TPL-CS-" + days : "TPL-NAT-" + days);
        if (plan.getStatus() == null) {
            plan.setStatus("执行中");
        }
        if (plan.getCareMode() == null) {
            plan.setCareMode("一对一专护");
        }
        planMapper.insert(plan);
        List<Template> templates = templateOf(deliveryType);
        LocalDate begin = startDate == null ? LocalDate.now() : startDate;
        for (int day = 0; day < days; day++) {
            for (Template tpl : templates) {
                CarePlanItem item = new CarePlanItem();
                item.setTenantId(plan.getTenantId());
                item.setPlanId(plan.getId());
                item.setItemName(tpl.name());
                item.setTargetType(tpl.target());
                item.setPlanDate(begin.plusDays(day));
                item.setTimeSlot(tpl.timeSlot());
                item.setStatus("待执行");
                itemMapper.insert(item);
            }
        }
        log.info("护理计划生成 planId={} days={} items={}", plan.getId(), days, days * templates.size());
        return plan.getId();
    }

    public List<CarePlan> plans(String status, Long orderId) {
        return planMapper.selectList(Wrappers.<CarePlan>lambdaQuery()
                .eq(status != null && !status.isBlank(), CarePlan::getStatus, status)
                .eq(orderId != null, CarePlan::getOrderId, orderId)
                .orderByDesc(CarePlan::getId));
    }

    /**
     * 护理师端「我的今日任务」：按护理员过滤计划项，并带出客户姓名、房间与护理模式。
     */
    public List<Map<String, Object>> itemsOfNurse(LocalDate date, Long nurseId) {
        List<CarePlanItem> items = itemMapper.selectList(Wrappers.<CarePlanItem>lambdaQuery()
                .eq(CarePlanItem::getPlanDate, date)
                .eq(nurseId != null, CarePlanItem::getNurseId, nurseId)
                .orderByAsc(CarePlanItem::getTimeSlot));
        Map<Long, CarePlan> planCache = new LinkedHashMap<>();
        List<Map<String, Object>> result = new ArrayList<>();
        for (CarePlanItem item : items) {
            CarePlan plan = planCache.computeIfAbsent(item.getPlanId(), planMapper::selectById);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", item.getId());
            row.put("planId", item.getPlanId());
            row.put("itemName", item.getItemName());
            row.put("targetType", item.getTargetType());
            row.put("timeSlot", item.getTimeSlot());
            row.put("status", item.getStatus());
            row.put("finishedTime", item.getFinishedTime());
            row.put("customerName", plan == null ? null : plan.getCustomerName());
            row.put("roomId", plan == null ? null : plan.getRoomId());
            row.put("careMode", plan == null ? null : plan.getCareMode());
            row.put("orderId", plan == null ? null : plan.getOrderId());
            result.add(row);
        }
        return result;
    }

    public Map<String, Object> planDetail(Long planId) {        CarePlan plan = requirePlan(planId);
        List<CarePlanItem> items = itemMapper.selectList(Wrappers.<CarePlanItem>lambdaQuery()
                .eq(CarePlanItem::getPlanId, planId)
                .orderByAsc(CarePlanItem::getPlanDate).orderByAsc(CarePlanItem::getTimeSlot));
        Map<String, List<CarePlanItem>> grouped = new LinkedHashMap<>();
        for (CarePlanItem item : items) {
            grouped.computeIfAbsent(item.getPlanDate().toString(), key -> new ArrayList<>()).add(item);
        }
        long done = items.stream().filter(i -> "已完成".equals(i.getStatus())).count();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("plan", plan);
        result.put("items", grouped);
        result.put("total", items.size());
        result.put("done", done);
        result.put("rate", items.isEmpty() ? 0 : Math.round(done * 100.0 / items.size()));
        return result;
    }

    @Transactional
    public void addItem(Long planId, CarePlanItem item) {
        requirePlan(planId);
        item.setTenantId(TenantContext.requireTenantId());
        item.setPlanId(planId);
        if (item.getStatus() == null) {
            item.setStatus("待执行");
        }
        itemMapper.insert(item);
    }

    @Transactional
    public void removeItem(Long itemId) {
        CarePlanItem item = requireItem(itemId);
        if ("已完成".equals(item.getStatus())) {
            throw BusinessException.of(ErrorCode.CONFLICT, "已完成的护理项不可删除");
        }
        itemMapper.deleteById(itemId);
    }

    /**
     * 执行护理项并写记录。
     */
    @Transactional
    public Long execute(Long itemId, CareRecord record) {
        CarePlanItem item = requireItem(itemId);
        if ("已完成".equals(item.getStatus())) {
            throw BusinessException.of(ErrorCode.CONFLICT, "该护理项已完成，如需修改请编辑记录");
        }
        LocalDateTime now = LocalDateTime.now();
        boolean overdue = item.getPlanDate().plusDays(1).atStartOfDay().isBefore(now);
        if (overdue && (record.getBackfillReason() == null || record.getBackfillReason().isBlank())) {
            throw BusinessException.of(ErrorCode.BAD_REQUEST, "超过 24 小时的补录必须填写补录原因");
        }
        item.setStatus("已完成");
        item.setFinishedTime(now);
        item.setNurseId(TenantContext.getUserId());
        itemMapper.updateById(item);

        CarePlan plan = requirePlan(item.getPlanId());
        record.setTenantId(item.getTenantId());
        record.setPlanItemId(itemId);
        record.setOrderId(plan.getOrderId());
        record.setItemName(item.getItemName());
        record.setTargetType(item.getTargetType());
        record.setNurseId(TenantContext.getUserId());
        record.setSubmitTime(now);
        record.setStatus(overdue ? "待审批" : "已提交");
        recordMapper.insert(record);
        return record.getId();
    }

    /**
     * 修改护理记录：提交后 24 小时内可直接改，超时进入待审批。
     */
    @Transactional
    public void updateRecord(Long recordId, CareRecord input) {
        CareRecord record = requireRecord(recordId);
        if (LocalDateTime.now().isAfter(record.getSubmitTime().plusHours(24))) {
            if (input.getBackfillReason() == null || input.getBackfillReason().isBlank()) {
                throw BusinessException.of(ErrorCode.BAD_REQUEST, "超过 24 小时修改必须填写原因并提交审批");
            }
            record.setStatus("待审批");
        }
        input.setId(record.getId());
        input.setTenantId(record.getTenantId());
        input.setOrderId(record.getOrderId());
        input.setPlanItemId(record.getPlanItemId());
        input.setNurseId(record.getNurseId());
        input.setSubmitTime(record.getSubmitTime());
        input.setStatus(record.getStatus());
        recordMapper.updateById(input);
    }

    @Transactional
    public void approveRecord(Long recordId) {
        CareRecord record = requireRecord(recordId);
        record.setStatus("已提交");
        recordMapper.updateById(record);
    }

    public List<CareRecord> records(Long orderId, String targetType) {
        return recordMapper.selectList(Wrappers.<CareRecord>lambdaQuery()
                .eq(orderId != null, CareRecord::getOrderId, orderId)
                .eq(targetType != null && !targetType.isBlank(), CareRecord::getTargetType, targetType)
                .orderByDesc(CareRecord::getSubmitTime).last("limit 200"));
    }

    /** 体征趋势：按提交时间升序返回体温、体重、黄疸测值。 */
    public List<Map<String, Object>> trend(Long orderId, String targetType) {
        List<CareRecord> rows = recordMapper.selectList(Wrappers.<CareRecord>lambdaQuery()
                .eq(CareRecord::getOrderId, orderId)
                .eq(targetType != null && !targetType.isBlank(), CareRecord::getTargetType, targetType)
                .orderByAsc(CareRecord::getSubmitTime));
        List<Map<String, Object>> points = new ArrayList<>();
        for (CareRecord row : rows) {
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("time", row.getSubmitTime());
            point.put("temperature", row.getTemperature());
            point.put("weight", row.getWeight());
            point.put("jaundice", row.getJaundiceValue());
            point.put("item", row.getItemName());
            points.add(point);
        }
        return points;
    }

    /**
     * 家属每日确认（小程序/H5）：把家属确认结果写入护理记录，供主管查房与质控回溯。
     *
     * <p>家属自助提交，nurse_id 固定记 0（表示非员工提交）；同一订单同一天只允许确认一次。</p>
     */
    @Transactional
    public Long confirmDaily(Long orderId, boolean satisfied, String remark) {
        Long dup = recordMapper.selectCount(Wrappers.<CareRecord>lambdaQuery()
                .eq(CareRecord::getOrderId, orderId)
                .eq(CareRecord::getItemName, "家属每日确认")
                .ge(CareRecord::getSubmitTime, LocalDate.now().atStartOfDay()));
        if (dup > 0) {
            throw BusinessException.of(ErrorCode.CONFLICT, "今日已确认过，如有新的诉求请通过「反馈」提交");
        }
        CareRecord record = new CareRecord();
        record.setTenantId(TenantContext.requireTenantId());
        record.setOrderId(orderId);
        record.setTargetType("产妇");
        record.setItemName("家属每日确认");
        record.setContent((satisfied ? "家属确认当日护理服务已完成" : "家属对当日护理服务有异议")
                + (remark == null || remark.isBlank() ? "" : "：" + remark.trim()));
        record.setNurseId(0L);
        record.setSubmitTime(LocalDateTime.now());
        record.setStatus("已提交");
        recordMapper.insert(record);
        log.info("家属每日确认 orderId={} satisfied={}", orderId, satisfied);
        return record.getId();
    }

    /* ---------------- 交接班 ---------------- */
    @Transactional
    public Long createHandover(CareHandover handover) {
        handover.setTenantId(TenantContext.requireTenantId());
        if (handover.getShiftDate() == null) {
            handover.setShiftDate(LocalDate.now());
        }
        if (handover.getFromNurseId() == null) {
            handover.setFromNurseId(TenantContext.getUserId());
        }
        handover.setStatus("待确认");
        handoverMapper.insert(handover);
        return handover.getId();
    }

    @Transactional
    public void confirmHandover(Long id, String signKey) {
        CareHandover handover = handoverMapper.selectById(id);
        if (handover == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "交接单不存在");
        }
        if ("已确认".equals(handover.getStatus())) {
            return;
        }
        handover.setToSign(signKey);
        handover.setConfirmedTime(LocalDateTime.now());
        handover.setStatus("已确认");
        handoverMapper.updateById(handover);
    }

    public List<CareHandover> handovers(LocalDate date) {
        return handoverMapper.selectList(Wrappers.<CareHandover>lambdaQuery()
                .eq(date != null, CareHandover::getShiftDate, date)
                .orderByDesc(CareHandover::getId));
    }

    /** 交接及时率：已确认 / 总数（按日期区间）。 */
    public Map<String, Object> handoverStats(LocalDate from, LocalDate to) {
        List<CareHandover> rows = handoverMapper.selectList(Wrappers.<CareHandover>lambdaQuery()
                .between(from != null, CareHandover::getShiftDate, from, to));
        long confirmed = rows.stream().filter(r -> "已确认".equals(r.getStatus())).count();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", rows.size());
        result.put("confirmed", confirmed);
        result.put("rate", rows.isEmpty() ? 100 : Math.round(confirmed * 100.0 / rows.size()));
        return result;
    }

    /* ---------------- 异常 ---------------- */

    @Transactional
    public Long reportAbnormal(CareAbnormal abnormal) {
        if (abnormal.getDescription() == null || abnormal.getDescription().isBlank()) {
            throw BusinessException.of(ErrorCode.BAD_REQUEST, "异常描述不能为空");
        }
        abnormal.setTenantId(TenantContext.requireTenantId());
        abnormal.setReporterId(TenantContext.getUserId());
        abnormal.setStatus("待处理");
        if (abnormal.getUrgency() == null) {
            abnormal.setUrgency("一般");
        }
        if (abnormal.getFamilyNotified() == null) {
            abnormal.setFamilyNotified(0);
        }
        abnormalMapper.insert(abnormal);
        log.warn("护理异常上报 orderId={} type={} urgency={}", abnormal.getOrderId(),
                abnormal.getAbnormalType(), abnormal.getUrgency());
        return abnormal.getId();
    }

    @Transactional
    public void handleAbnormal(Long id, CareAbnormal input) {
        CareAbnormal abnormal = abnormalMapper.selectById(id);
        if (abnormal == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "异常单不存在");
        }
        abnormal.setHandleResult(input.getHandleResult());
        abnormal.setSupervisorId(input.getSupervisorId() == null
                ? TenantContext.getUserId() : input.getSupervisorId());
        abnormal.setFamilyNotified(input.getFamilyNotified());
        boolean closed = input.getStatus() != null && "已闭环".equals(input.getStatus());
        abnormal.setStatus(closed ? "已闭环" : "处理中");
        if ("已闭环".equals(abnormal.getStatus())) {
            abnormal.setClosedTime(LocalDateTime.now());
        }
        abnormalMapper.updateById(abnormal);
    }

    public List<CareAbnormal> abnormals(String status, String urgency) {
        return abnormalMapper.selectList(Wrappers.<CareAbnormal>lambdaQuery()
                .eq(status != null && !status.isBlank(), CareAbnormal::getStatus, status)
                .eq(urgency != null && !urgency.isBlank(), CareAbnormal::getUrgency, urgency)
                .orderByAsc(CareAbnormal::getStatus).orderByDesc(CareAbnormal::getId));
    }

    /** 护理看板：今日完成率与异常数量（T55）。 */
    public Map<String, Object> board(LocalDate date) {
        LocalDate day = date == null ? LocalDate.now() : date;
        List<CarePlanItem> items = itemMapper.selectList(Wrappers.<CarePlanItem>lambdaQuery()
                .eq(CarePlanItem::getPlanDate, day));
        long done = items.stream().filter(i -> "已完成".equals(i.getStatus())).count();
        long abnormal = abnormalMapper.selectCount(Wrappers.<CareAbnormal>lambdaQuery()
                .ne(CareAbnormal::getStatus, "已闭环"));
        List<Map<String, Object>> cards = new ArrayList<>();
        Map<Long, List<CarePlanItem>> byPlan = new LinkedHashMap<>();
        for (CarePlanItem item : items) {
            byPlan.computeIfAbsent(item.getPlanId(), key -> new ArrayList<>()).add(item);
        }
        for (Map.Entry<Long, List<CarePlanItem>> entry : byPlan.entrySet()) {
            CarePlan plan = planMapper.selectById(entry.getKey());
            if (plan == null) {
                continue;
            }
            long planDone = entry.getValue().stream().filter(i -> "已完成".equals(i.getStatus())).count();
            Map<String, Object> card = new LinkedHashMap<>();
            card.put("planId", plan.getId());
            card.put("customerName", plan.getCustomerName());
            card.put("roomId", plan.getRoomId());
            card.put("careMode", plan.getCareMode());
            card.put("total", entry.getValue().size());
            card.put("done", planDone);
            card.put("rate", entry.getValue().isEmpty() ? 0
                    : Math.round(planDone * 100.0 / entry.getValue().size()));
            card.put("abnormal", entry.getValue().stream().anyMatch(i -> "已跳过".equals(i.getStatus())));
            cards.add(card);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("date", day.toString());
        result.put("total", items.size());
        result.put("done", done);
        result.put("rate", items.isEmpty() ? 0 : Math.round(done * 100.0 / items.size()));
        result.put("openAbnormal", abnormal);
        result.put("cards", cards);
        return result;
    }

    public CarePlan requirePlan(Long id) {
        CarePlan plan = planMapper.selectById(id);
        if (plan == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "护理计划不存在");
        }
        return plan;
    }

    public CarePlanItem requireItem(Long id) {
        CarePlanItem item = itemMapper.selectById(id);
        if (item == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "护理项不存在");
        }
        return item;
    }

    public CareRecord requireRecord(Long id) {
        CareRecord record = recordMapper.selectById(id);
        if (record == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "护理记录不存在");
        }
        return record;
    }

    /** 模板行：对象、项目、时段。 */
    record Template(String target, String name, String timeSlot) {
    }
}
