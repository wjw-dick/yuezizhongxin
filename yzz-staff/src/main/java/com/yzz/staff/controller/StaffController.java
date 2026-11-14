package com.yzz.staff.controller;

import com.yzz.common.api.R;
import com.yzz.common.tenant.AuthGuard;
import com.yzz.staff.entity.StaffAttendance;
import com.yzz.staff.entity.StaffCert;
import com.yzz.staff.entity.StaffInfo;
import com.yzz.staff.entity.StaffSchedule;
import com.yzz.staff.entity.StaffShift;
import com.yzz.staff.service.StaffService;
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
 * 人员、资质、排班与考勤（T46）。
 */
@Tag(name = "租户-人员与排班")
@RestController
@RequestMapping("/api/tenant/staff")
public class StaffController {

    private final StaffService staffService;

    public StaffController(StaffService staffService) {
        this.staffService = staffService;
    }

    @Operation(summary = "人员列表")
    @GetMapping
    public R<List<StaffInfo>> staffs(@RequestParam(required = false) String jobType,
                                     @RequestParam(required = false) String status) {
        AuthGuard.requireTenant();
        return R.ok(staffService.staffs(jobType, status));
    }

    @Operation(summary = "保存人员档案")
    @PostMapping
    public R<Long> save(@RequestBody StaffInfo request) {
        AuthGuard.requireTenant();
        return R.ok(staffService.saveStaff(request));
    }

    @Operation(summary = "资质证件列表")
    @GetMapping("/{staffId}/certs")
    public R<List<StaffCert>> certs(@PathVariable Long staffId) {
        AuthGuard.requireTenant();
        return R.ok(staffService.certs(staffId));
    }

    @Operation(summary = "新增资质证件")
    @PostMapping("/certs")
    public R<Long> addCert(@RequestBody StaffCert request) {
        AuthGuard.requireTenant();
        return R.ok(staffService.addCert(request));
    }

    @Operation(summary = "证件到期提醒")
    @GetMapping("/certs/expiring")
    public R<List<Map<String, Object>>> expiring(@RequestParam(defaultValue = "60") int days) {
        AuthGuard.requireTenant();
        return R.ok(staffService.expiringCerts(days));
    }

    @Operation(summary = "班次定义")
    @GetMapping("/shifts")
    public R<List<StaffShift>> shifts() {
        AuthGuard.requireTenant();
        return R.ok(staffService.shifts());
    }

    @Operation(summary = "保存班次")
    @PostMapping("/shifts")
    public R<Long> saveShift(@RequestBody StaffShift request) {
        AuthGuard.requireTenant();
        return R.ok(staffService.saveShift(request));
    }

    @Operation(summary = "排班列表")
    @GetMapping("/schedules")
    public R<List<StaffSchedule>> schedules(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long staffId) {
        AuthGuard.requireTenant();
        return R.ok(staffService.schedules(from, to, staffId));
    }

    @Operation(summary = "批量排班（一对一配比校验）")
    @PostMapping("/schedules")
    public R<Integer> saveSchedules(@RequestBody List<StaffSchedule> rows) {
        AuthGuard.requireTenant();
        return R.ok(staffService.saveSchedules(rows));
    }

    @Operation(summary = "打卡")
    @PostMapping("/attendance")
    public R<Void> attendance(@RequestBody Map<String, String> body) {
        AuthGuard.requireTenant();
        staffService.attendance(Long.valueOf(body.get("staffId")),
                body.getOrDefault("type", "in"), body.get("location"));
        return R.ok();
    }

    @Operation(summary = "考勤记录")
    @GetMapping("/attendance")
    public R<List<StaffAttendance>> attendanceOf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long staffId) {
        AuthGuard.requireTenant();
        return R.ok(staffService.attendanceOf(from, to, staffId));
    }

    @Operation(summary = "工作量统计")
    @GetMapping("/workload")
    public R<List<Map<String, Object>>> workload(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        AuthGuard.requireTenant();
        LocalDate start = from == null ? LocalDate.now().withDayOfMonth(1) : from;
        LocalDate end = to == null ? LocalDate.now() : to;
        return R.ok(staffService.workload(start, end));
    }
}
