package com.yzz.api.web;

import com.yzz.care.entity.CareAbnormal;
import com.yzz.care.service.CareService;
import com.yzz.common.api.BusinessException;
import com.yzz.common.api.ErrorCode;
import com.yzz.common.api.R;
import com.yzz.common.tenant.TenantContext;
import com.yzz.crm.entity.CrmLead;
import com.yzz.crm.service.LeadService;
import com.yzz.meal.entity.MealOrder;
import com.yzz.meal.entity.MealRestrict;
import com.yzz.meal.service.MealService;
import com.yzz.member.entity.MktReferral;
import com.yzz.member.service.MarketingService;
import com.yzz.member.service.MemberAuthService;
import com.yzz.member.service.MemberService;
import com.yzz.order.service.OrderService;
import com.yzz.rehab.entity.RehabAppointment;
import com.yzz.rehab.service.RehabService;
import com.yzz.room.entity.Room;
import com.yzz.room.mapper.RoomMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 小程序 / H5 / App 客户自助服务（T74、T76、T77、T79）。
 *
 * <p>订单、在住、护理、月子餐、康复全部按登录会员手机号鉴权：
 * 请求头 {@code X-Member-Token} 换出会员，会员手机号必须与订单下单手机号一致，
 * 否则返回 40304，杜绝按订单号/订单主键越权读取他人数据。</p>
 */
@Tag(name = "开放接口-客户自助服务")
@RestController
@RequestMapping("/api/open/customer")
public class OpenCustomerController {

    private final MemberAuthService authService;
    private final MemberService memberService;
    private final OrderService orderService;
    private final CareService careService;
    private final MealService mealService;
    private final RehabService rehabService;
    private final LeadService leadService;
    private final MarketingService marketingService;
    private final RoomMapper roomMapper;

    public OpenCustomerController(MemberAuthService authService, MemberService memberService,
                                  OrderService orderService, CareService careService,
                                  MealService mealService, RehabService rehabService,
                                  LeadService leadService, MarketingService marketingService,
                                  RoomMapper roomMapper) {
        this.authService = authService;
        this.memberService = memberService;
        this.orderService = orderService;
        this.careService = careService;
        this.mealService = mealService;
        this.rehabService = rehabService;
        this.leadService = leadService;
        this.marketingService = marketingService;
        this.roomMapper = roomMapper;
    }

    @Operation(summary = "我的订单列表")
    @GetMapping("/orders")
    public R<List<Map<String, Object>>> orders(@RequestHeader("X-Member-Token") String token,
                                               @RequestParam(required = false) String status) {
        String phone = phoneOf(token);
        return R.ok(orderService.listForCustomer(phone, status));
    }

    @Operation(summary = "我的订单详情")
    @GetMapping("/orders/{orderNo}")
    public R<Map<String, Object>> orderDetail(@RequestHeader("X-Member-Token") String token,
                                              @PathVariable String orderNo) {
        return R.ok(orderService.detailForCustomer(orderNo, phoneOf(token)));
    }

    @Operation(summary = "申请退订")
    @PostMapping("/orders/{orderNo}/refund")
    public R<Map<String, Object>> refund(@RequestHeader("X-Member-Token") String token,
                                         @PathVariable String orderNo,
                                         @RequestBody Map<String, String> body) {
        return R.ok(orderService.applyRefundForCustomer(orderNo, phoneOf(token), body.get("reason")));
    }

    @Operation(summary = "当前在住概览（房间、入离馆、护理计划进度）")
    @GetMapping("/stay")
    public R<Map<String, Object>> stay(@RequestHeader("X-Member-Token") String token) {
        String phone = phoneOf(token);
        List<Map<String, Object>> living = orderService.listForCustomer(phone, "在住");
        Map<String, Object> result = new LinkedHashMap<>();
        if (living.isEmpty()) {
            result.put("living", false);
            return R.ok(result);
        }
        Map<String, Object> current = living.get(0);
        Long orderId = ((Number) current.get("id")).longValue();
        result.put("living", true);
        result.put("order", current);
        Object roomId = current.get("roomId");
        if (roomId instanceof Number number) {
            Room room = roomMapper.selectById(number.longValue());
            result.put("roomNo", room == null ? null : room.getRoomNo());
        }
        result.put("care", careSummary(orderId));
        return R.ok(result);
    }

    @Operation(summary = "护理计划与记录（客户视角）")
    @GetMapping("/care")
    public R<Map<String, Object>> care(@RequestHeader("X-Member-Token") String token,
                                       @RequestParam Long orderId) {
        requireOrderOwner(token, orderId);
        return R.ok(careSummary(orderId));
    }

    @Operation(summary = "家属每日确认")
    @PostMapping("/care/confirm")
    public R<Long> confirmDaily(@RequestHeader("X-Member-Token") String token,
                                @RequestBody Map<String, Object> body) {
        Long orderId = Long.valueOf(String.valueOf(body.get("orderId")));
        requireOrderOwner(token, orderId);
        boolean satisfied = Boolean.parseBoolean(String.valueOf(body.getOrDefault("satisfied", true)));
        String remark = body.get("remark") == null ? null : String.valueOf(body.get("remark"));
        return R.ok(careService.confirmDaily(orderId, satisfied, remark));
    }

    @Operation(summary = "家属反馈异常/诉求")
    @PostMapping("/care/feedback")
    public R<Long> feedback(@RequestHeader("X-Member-Token") String token,
                            @RequestBody Map<String, String> body) {
        Long orderId = body.get("orderId") == null ? null : Long.valueOf(body.get("orderId"));
        if (orderId == null) {
            throw BusinessException.of(ErrorCode.BAD_REQUEST, "请先选择在住订单");
        }
        requireOrderOwner(token, orderId);
        CareAbnormal abnormal = new CareAbnormal();
        abnormal.setOrderId(orderId);
        abnormal.setAbnormalType(body.getOrDefault("abnormalType", "家属反馈"));
        abnormal.setUrgency(body.getOrDefault("urgency", "一般"));
        abnormal.setDescription(body.get("description"));
        abnormal.setFamilyNotified(1);
        return R.ok(careService.reportAbnormal(abnormal));
    }

    @Operation(summary = "今日月子餐与忌口")
    @GetMapping("/meal")
    public R<Map<String, Object>> meal(@RequestHeader("X-Member-Token") String token,
                                       @RequestParam Long orderId,
                                       @RequestParam(required = false) String date) {
        requireOrderOwner(token, orderId);
        LocalDate day = date == null || date.isBlank() ? LocalDate.now() : LocalDate.parse(date);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("date", day);
        result.put("meals", mealService.meals(day, orderId, null));
        result.put("restricts", mealService.restricts(orderId));
        return R.ok(result);
    }

    @Operation(summary = "申请加餐（单独计费）")
    @PostMapping("/meal/addon")
    public R<Long> addOn(@RequestHeader("X-Member-Token") String token,
                         @RequestBody Map<String, Object> body) {
        Long orderId = Long.valueOf(String.valueOf(body.get("orderId")));
        requireOrderOwner(token, orderId);
        String date = body.get("date") == null ? null : String.valueOf(body.get("date"));
        BigDecimal charge = body.get("charge") == null ? BigDecimal.ZERO
                : new BigDecimal(String.valueOf(body.get("charge")));
        return R.ok(mealService.addOn(orderId, date == null ? LocalDate.now() : LocalDate.parse(date),
                String.valueOf(body.getOrDefault("mealType", "加餐")),
                String.valueOf(body.get("dishes")), charge));
    }

    @Operation(summary = "忌口/过敏原自助登记")
    @PostMapping("/meal/restrict")
    public R<Long> addRestrict(@RequestHeader("X-Member-Token") String token,
                               @RequestBody Map<String, String> body) {
        Long orderId = Long.valueOf(body.get("orderId"));
        requireOrderOwner(token, orderId);
        MealRestrict restrict = new MealRestrict();
        restrict.setOrderId(orderId);
        restrict.setRestrictType(body.getOrDefault("restrictType", "忌口"));
        restrict.setContent(body.get("content"));
        return R.ok(mealService.addRestrict(restrict));
    }

    @Operation(summary = "康复疗程余额、评估与预约")
    @GetMapping("/rehab")
    public R<Map<String, Object>> rehab(@RequestHeader("X-Member-Token") String token,
                                        @RequestParam Long orderId) {
        requireOrderOwner(token, orderId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("courses", rehabService.courses(orderId));
        result.put("assesses", rehabService.assesses(orderId));
        result.put("appointments", rehabService.appointmentsOfOrder(orderId));
        result.put("records", rehabService.records(orderId));
        return R.ok(result);
    }

    @Operation(summary = "自助预约康复项目（待门店确认）")
    @PostMapping("/rehab/appoint")
    public R<Long> appoint(@RequestHeader("X-Member-Token") String token,
                           @RequestBody Map<String, String> body) {
        Long orderId = Long.valueOf(body.get("orderId"));
        requireOrderOwner(token, orderId);
        return R.ok(rehabService.requestAppoint(orderId,
                Long.valueOf(body.get("itemId")),
                LocalDate.parse(body.get("date")),
                body.get("startTime"), body.get("endTime")));
    }

    @Operation(summary = "取消康复预约")
    @PostMapping("/rehab/cancel")
    public R<Void> cancelAppoint(@RequestHeader("X-Member-Token") String token,
                                 @RequestBody Map<String, String> body) {
        Long orderId = Long.valueOf(body.get("orderId"));
        requireOrderOwner(token, orderId);
        rehabService.cancelByCustomer(orderId, Long.valueOf(body.get("appointmentId")));
        return R.ok();
    }

    @Operation(summary = "提交转介绍（被推荐人进线索池）")
    @PostMapping("/referral")
    public R<Map<String, Object>> referral(@RequestHeader("X-Member-Token") String token,
                                           @RequestBody Map<String, String> body) {
        Long memberId = authService.resolve(token);
        String name = body.get("name");
        String phone = body.get("phone");
        CrmLead lead = new CrmLead();
        lead.setCustomerName(name);
        lead.setPhone(phone);
        lead.setSourceCode("老客转介绍");
        lead.setRemark("推荐人会员ID：" + memberId);
        Long leadId = leadService.create(lead);
        Long referralId = marketingService.refer(memberId, leadId, 500, BigDecimal.ZERO);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("leadId", leadId);
        result.put("referralId", referralId);
        result.put("rewardPoints", 500);
        result.put("tip", "被推荐人到店签约后 500 积分自动入账");
        return R.ok(result);
    }

    @Operation(summary = "我的转介绍记录")
    @GetMapping("/referrals")
    public R<List<MktReferral>> referrals(@RequestHeader("X-Member-Token") String token) {
        Long memberId = authService.resolve(token);
        return R.ok(marketingService.referrals().stream()
                .map(row -> row.get("referral"))
                .filter(MktReferral.class::isInstance)
                .map(MktReferral.class::cast)
                .filter(row -> memberId.equals(row.getReferrerMember()))
                .toList());
    }

    /* ---------------- 内部方法 ---------------- */

    private Map<String, Object> careSummary(Long orderId) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> plans = careService.plans(null, orderId).stream()
                .map(plan -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", plan.getId());
                    row.put("status", plan.getStatus());
                    row.put("careMode", plan.getCareMode());
                    row.put("createdTime", plan.getCreatedTime());
                    return row;
                }).toList();
        result.put("plans", plans);
        if (plans.isEmpty()) {
            result.put("today", List.of());
            result.put("rate", 0);
            result.put("records", List.of());
            result.put("trendMom", List.of());
            result.put("trendBaby", List.of());
            return result;
        }
        Long planId = ((Number) plans.get(0).get("id")).longValue();
        Map<String, Object> detail = careService.planDetail(planId);
        result.put("planId", planId);
        result.put("total", detail.get("total"));
        result.put("done", detail.get("done"));
        result.put("rate", detail.get("rate"));
        @SuppressWarnings("unchecked")
        Map<String, List<Object>> grouped = (Map<String, List<Object>>) detail.get("items");
        result.put("grouped", grouped);
        result.put("today", grouped.getOrDefault(LocalDate.now().toString(), List.of()));
        result.put("records", careService.records(orderId, null));
        result.put("trendMom", careService.trend(orderId, "产妇"));
        result.put("trendBaby", careService.trend(orderId, "新生儿"));
        return result;
    }

    private String phoneOf(String token) {
        resolveTenant();
        Long memberId = authService.resolve(token);
        String phone = memberService.plainPhone(memberId);
        if (phone == null) {
            throw BusinessException.of(ErrorCode.UNAUTHORIZED, "会员手机号缺失，请重新登录");
        }
        return phone;
    }

    private void requireOrderOwner(String token, Long orderId) {
        String phone = phoneOf(token);
        // 以「会员手机号 = 订单下单手机号」为准鉴权，避免按订单主键遍历他人数据
        boolean owned = orderService.listForCustomer(phone, null).stream()
                .anyMatch(row -> ((Number) row.get("id")).longValue() == orderId);
        if (!owned) {
            throw BusinessException.of(ErrorCode.FORBIDDEN, "无权查看该订单");
        }
    }

    private void resolveTenant() {
        if (TenantContext.getTenantId() == null) {
            throw BusinessException.of(ErrorCode.TENANT_NOT_FOUND);
        }
    }

    /** 兼容前端按数组下标渲染，空列表返回可变集合。 */
    @SuppressWarnings("unused")
    private List<Object> emptyList() {
        return new ArrayList<>();
    }
}
