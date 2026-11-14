package com.yzz.api.web;

import com.yzz.care.entity.CareAbnormal;
import com.yzz.care.entity.CareHandover;
import com.yzz.care.entity.CareRecord;
import com.yzz.care.service.CareService;
import com.yzz.common.api.BusinessException;
import com.yzz.common.api.ErrorCode;
import com.yzz.common.api.R;
import com.yzz.common.guard.Idempotent;
import com.yzz.common.tenant.AuthGuard;
import com.yzz.common.tenant.TenantContext;
import com.yzz.common.util.SecretCipher;
import com.yzz.meal.entity.MealOrder;
import com.yzz.meal.service.MealService;
import com.yzz.staff.entity.StaffAttendance;
import com.yzz.staff.entity.StaffInfo;
import com.yzz.staff.service.StaffService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 护理师端小程序接口（T73、T79）。
 *
 * <p>与客户小程序物理隔离：必须租户员工登录态；接口不返回任何价格、支付、客户完整手机号，
 * 只返回执行护理所需字段（客户姓名、房间、护理项、忌口）。</p>
 */
@Tag(name = "员工端-护理师工作台")
@RestController
@RequestMapping("/api/tenant/mobile")
public class StaffMobileController {

    private final StaffService staffService;
    private final CareService careService;
    private final MealService mealService;
    private final SecretCipher secretCipher;

    public StaffMobileController(StaffService staffService, CareService careService,
                                 MealService mealService, SecretCipher secretCipher) {
        this.staffService = staffService;
        this.careService = careService;
        this.mealService = mealService;
        this.secretCipher = secretCipher;
    }

    @Operation(summary = "今日工作台")
    @GetMapping("/workbench")
    public R<Map<String, Object>> workbench(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        AuthGuard.requireTenant();
        StaffInfo staff = currentStaff();
        LocalDate day = date == null ? LocalDate.now() : date;
        List<Map<String, Object>> items = careService.itemsOfNurse(day, staff.getId());
        List<CareHandover> handovers = careService.handovers(day);
        List<CareAbnormal> abnormals = careService.abnormals("待处置", null);
        List<MealOrder> meals = mealService.meals(day, null, 0);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("staff", staff);
        result.put("date", day);
        result.put("attendance", todayAttendance(staff.getId()));
        result.put("items", items);
        result.put("doneCount", items.stream().filter(i -> "已完成".equals(i.get("status"))).count());
        result.put("totalCount", items.size());
        result.put("handovers", handovers);
        result.put("abnormals", abnormals);
        result.put("meals", meals);
        // 客户数：本人今日任务涉及的客户（按订单去重）
        result.put("customerCount", items.stream()
                .map(i -> i.get("orderId")).filter(java.util.Objects::nonNull).distinct().count());
        result.put("pendingMeals", meals.size());
        return R.ok(result);
    }

    @Operation(summary = "我的护理任务（按日期）")
    @GetMapping("/items")
    public R<List<Map<String, Object>>> items(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        AuthGuard.requireTenant();
        return R.ok(careService.itemsOfNurse(date == null ? LocalDate.now() : date,
                currentStaff().getId()));
    }

    @Operation(summary = "护理项详情（含客户与该客户最近记录）")
    @GetMapping("/items/{itemId}")
    public R<Map<String, Object>> item(@PathVariable Long itemId) {
        AuthGuard.requireTenant();
        com.yzz.care.entity.CarePlanItem item = careService.requireItem(itemId);
        com.yzz.care.entity.CarePlan plan = careService.requirePlan(item.getPlanId());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("item", item);
        result.put("plan", plan);
        result.put("customerName", plan.getCustomerName());
        result.put("careMode", plan.getCareMode());
        result.put("orderId", plan.getOrderId());
        result.put("records", careService.records(plan.getOrderId(), null).stream()
                .filter(r -> itemId.equals(r.getPlanItemId()))
                .limit(10).toList());
        result.put("recent", careService.records(plan.getOrderId(), null).stream()
                .limit(5).toList());
        return R.ok(result);
    }

    @Operation(summary = "执行护理项并提交记录")
    @Idempotent(ttlSeconds = 10)
    @PostMapping("/items/{itemId}/execute")
    public R<Long> execute(@PathVariable Long itemId, @RequestBody CareRecord record) {
        AuthGuard.requireTenant();
        record.setNurseId(currentStaff().getId());
        return R.ok(careService.execute(itemId, record));
    }

    @Operation(summary = "补录/修改护理记录（超 24 小时须填原因，进入待审批）")
    @PutMapping("/records/{recordId}")
    public R<Void> updateRecord(@PathVariable Long recordId, @RequestBody CareRecord record) {
        AuthGuard.requireTenant();
        record.setNurseId(currentStaff().getId());
        careService.updateRecord(recordId, record);
        return R.ok();
    }

    @Operation(summary = "交接班列表")
    @GetMapping("/handovers")
    public R<List<CareHandover>> handovers(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        AuthGuard.requireTenant();
        return R.ok(careService.handovers(date == null ? LocalDate.now() : date));
    }

    @Operation(summary = "创建交接单")
    @PostMapping("/handovers")
    public R<Long> createHandover(@RequestBody CareHandover handover) {
        AuthGuard.requireTenant();
        handover.setFromNurseId(currentStaff().getId());
        return R.ok(careService.createHandover(handover));
    }

    @Operation(summary = "接班确认")
    @PostMapping("/handovers/{id}/confirm")
    public R<Void> confirmHandover(@PathVariable Long id, @RequestBody Map<String, String> body) {
        AuthGuard.requireTenant();
        careService.confirmHandover(id, body.get("signKey"));
        return R.ok();
    }

    @Operation(summary = "异常上报")
    @PostMapping("/abnormals")
    public R<Long> reportAbnormal(@RequestBody CareAbnormal abnormal) {
        AuthGuard.requireTenant();
        abnormal.setReporterId(TenantContext.getUserId());
        return R.ok(careService.reportAbnormal(abnormal));
    }

    @Operation(summary = "待处置异常")
    @GetMapping("/abnormals")
    public R<List<CareAbnormal>> abnormals(@RequestParam(required = false) String status) {
        AuthGuard.requireTenant();
        return R.ok(careService.abnormals(status == null ? "待处置" : status, null));
    }

    @Operation(summary = "出餐核对列表")
    @GetMapping("/meals")
    public R<List<MealOrder>> meals(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Integer checked) {
        AuthGuard.requireTenant();
        return R.ok(mealService.meals(date == null ? LocalDate.now() : date, null, checked));
    }

    @Operation(summary = "出餐核对（异常必填说明）")
    @PostMapping("/meals/{id}/check")
    public R<Void> checkMeal(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        AuthGuard.requireTenant();
        mealService.check(id, body == null ? null : body.get("abnormal"));
        return R.ok();
    }

    @Operation(summary = "考勤打卡")
    @PostMapping("/attendance")
    public R<Void> attendance(@RequestBody Map<String, String> body) {
        AuthGuard.requireTenant();
        staffService.attendance(currentStaff().getId(),
                body.getOrDefault("type", "in"), body.get("location"));
        return R.ok();
    }

    @Operation(summary = "我的考勤记录")
    @GetMapping("/attendance")
    public R<List<StaffAttendance>> attendance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        AuthGuard.requireTenant();
        LocalDate end = to == null ? LocalDate.now() : to;
        LocalDate start = from == null ? end.withDayOfMonth(1) : from;
        return R.ok(staffService.attendanceOf(start, end, currentStaff().getId()));
    }

    @Operation(summary = "我的员工档案（不含薪酬）")
    @GetMapping("/profile")
    public R<StaffInfo> profile() {
        AuthGuard.requireTenant();
        StaffInfo staff = currentStaff();
        staff.setPhone(maskPhone(staff.getPhone()));
        return R.ok(staff);
    }

    /* ---------------- 内部方法 ---------------- */

    private StaffInfo currentStaff() {
        StaffInfo staff = staffService.byUser(TenantContext.getUserId());
        if (staff == null) {
            throw BusinessException.of(ErrorCode.FORBIDDEN,
                    "当前账号未绑定在职员工档案，请联系主管在「员工档案」中绑定登录账号");
        }
        return staff;
    }

    private StaffAttendance todayAttendance(Long staffId) {
        List<StaffAttendance> rows = staffService.attendanceOf(LocalDate.now(), LocalDate.now(), staffId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private String maskPhone(String encrypted) {
        try {
            String plain = secretCipher.decrypt(encrypted);
            return com.yzz.common.util.DesensitizeUtil.phone(plain);
        } catch (Exception e) {
            return encrypted;
        }
    }
}
