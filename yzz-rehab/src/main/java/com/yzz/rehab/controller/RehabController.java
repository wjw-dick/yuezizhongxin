package com.yzz.rehab.controller;

import com.yzz.common.api.R;
import com.yzz.common.tenant.AuthGuard;
import com.yzz.rehab.entity.RehabAppointment;
import com.yzz.rehab.entity.RehabAssess;
import com.yzz.rehab.entity.RehabItem;
import com.yzz.rehab.entity.RehabRecord;
import com.yzz.rehab.service.RehabService;
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
 * 康复项目库、评估、疗程、预约与执行（T34、T44）。
 */
@Tag(name = "租户-产后康复")
@RestController
@RequestMapping("/api/tenant/rehab")
public class RehabController {

    private final RehabService rehabService;

    public RehabController(RehabService rehabService) {
        this.rehabService = rehabService;
    }

    @Operation(summary = "康复项目列表")
    @GetMapping("/items")
    public R<List<RehabItem>> items(@RequestParam(required = false) String stage,
                                    @RequestParam(required = false) String status) {
        AuthGuard.requireTenant();
        return R.ok(rehabService.items(stage, status));
    }

    @Operation(summary = "保存康复项目")
    @PostMapping("/items")
    public R<Long> saveItem(@RequestBody RehabItem request) {
        AuthGuard.requireTenant();
        return R.ok(rehabService.saveItem(request));
    }

    @Operation(summary = "康复评估列表")
    @GetMapping("/assesses")
    public R<List<RehabAssess>> assesses(@RequestParam(required = false) Long orderId) {
        AuthGuard.requireTenant();
        return R.ok(rehabService.assesses(orderId));
    }

    @Operation(summary = "提交康复评估")
    @PostMapping("/assesses")
    public R<Long> saveAssess(@RequestBody RehabAssess request) {
        AuthGuard.requireTenant();
        return R.ok(rehabService.saveAssess(request));
    }

    @Operation(summary = "疗程卡列表（含余额）")
    @GetMapping("/courses")
    public R<List<Map<String, Object>>> courses(@RequestParam(required = false) Long orderId) {
        AuthGuard.requireTenant();
        return R.ok(rehabService.courses(orderId));
    }

    @Operation(summary = "生成疗程卡")
    @PostMapping("/courses")
    public R<Long> createCourse(@RequestBody Map<String, Object> body) {
        AuthGuard.requireTenant();
        Long orderId = Long.valueOf(String.valueOf(body.get("orderId")));
        Long itemId = Long.valueOf(String.valueOf(body.get("itemId")));
        Integer times = body.get("totalTimes") == null ? null
                : Integer.valueOf(String.valueOf(body.get("totalTimes")));
        return R.ok(rehabService.createCourse(orderId, itemId, times, (String) body.get("source")));
    }

    @Operation(summary = "康复预约列表")
    @GetMapping("/appointments")
    public R<List<RehabAppointment>> appointments(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long therapistId) {
        AuthGuard.requireTenant();
        return R.ok(rehabService.appointments(date, therapistId));
    }

    @Operation(summary = "新建康复预约（校验治疗师与康复室冲突）")
    @PostMapping("/appointments")
    public R<Long> appoint(@RequestBody RehabAppointment request) {
        AuthGuard.requireTenant();
        return R.ok(rehabService.appoint(request));
    }

    @Operation(summary = "取消预约")
    @PostMapping("/appointments/{id}/cancel")
    public R<Void> cancel(@PathVariable Long id) {
        AuthGuard.requireTenant();
        rehabService.cancelAppoint(id);
        return R.ok();
    }

    @Operation(summary = "签到")
    @PostMapping("/appointments/{id}/checkin")
    public R<Void> checkin(@PathVariable Long id) {
        AuthGuard.requireTenant();
        rehabService.checkin(id);
        return R.ok();
    }

    @Operation(summary = "执行并扣次")
    @PostMapping("/appointments/{id}/execute")
    public R<Long> execute(@PathVariable Long id, @RequestBody RehabRecord request) {
        AuthGuard.requireTenant();
        return R.ok(rehabService.execute(id, request));
    }

    @Operation(summary = "执行记录")
    @GetMapping("/records")
    public R<List<RehabRecord>> records(@RequestParam Long orderId) {
        AuthGuard.requireTenant();
        return R.ok(rehabService.records(orderId));
    }
}
