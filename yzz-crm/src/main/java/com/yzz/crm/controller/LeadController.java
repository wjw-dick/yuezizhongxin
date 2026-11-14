package com.yzz.crm.controller;

import com.yzz.common.api.R;
import com.yzz.common.tenant.AuthGuard;
import com.yzz.crm.entity.CrmFollow;
import com.yzz.crm.entity.CrmLead;
import com.yzz.crm.entity.CrmVisit;
import com.yzz.crm.service.LeadService;
import com.yzz.order.dto.OrderCreateRequest;
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
import java.util.List;
import java.util.Map;

/**
 * 线索、跟进、参观与转订单（T35）。
 */
@Tag(name = "租户-线索 CRM")
@RestController
@RequestMapping("/api/tenant/crm")
public class LeadController {

    private final LeadService leadService;

    public LeadController(LeadService leadService) {
        this.leadService = leadService;
    }

    @Operation(summary = "线索列表")
    @GetMapping("/leads")
    public R<List<CrmLead>> leads(@RequestParam(required = false) String stage,
                                  @RequestParam(required = false) String sourceCode,
                                  @RequestParam(required = false) String keyword,
                                  @RequestParam(required = false) Long advisorId) {
        AuthGuard.requireTenant();
        return R.ok(leadService.list(stage, sourceCode, keyword, advisorId));
    }

    @Operation(summary = "线索详情")
    @GetMapping("/leads/{id}")
    public R<CrmLead> lead(@PathVariable Long id) {
        AuthGuard.requireTenant();
        return R.ok(leadService.detail(id));
    }

    @Operation(summary = "新增线索")
    @PostMapping("/leads")
    public R<Long> create(@RequestBody CrmLead request) {
        AuthGuard.requireTenant();
        return R.ok(leadService.create(request));
    }

    @Operation(summary = "修改线索")
    @PutMapping("/leads/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody CrmLead request) {
        AuthGuard.requireTenant();
        leadService.update(id, request);
        return R.ok();
    }

    @Operation(summary = "跟进记录")
    @GetMapping("/leads/{id}/follows")
    public R<List<CrmFollow>> follows(@PathVariable Long id) {
        AuthGuard.requireTenant();
        return R.ok(leadService.follows(id));
    }

    @Operation(summary = "新增跟进")
    @PostMapping("/leads/{id}/follows")
    public R<Void> addFollow(@PathVariable Long id, @RequestBody CrmFollow request) {
        AuthGuard.requireTenant();
        leadService.addFollow(id, request);
        return R.ok();
    }

    @Operation(summary = "跟进超时列表")
    @GetMapping("/leads/overdue")
    public R<List<CrmLead>> overdue() {
        AuthGuard.requireTenant();
        return R.ok(leadService.overdueFollows());
    }

    @Operation(summary = "一键转订单")
    @PostMapping("/leads/{id}/convert")
    public R<Map<String, Object>> convert(@PathVariable Long id, @RequestBody OrderCreateRequest request) {
        AuthGuard.requireTenant();
        return R.ok(leadService.convert(id, request));
    }

    @Operation(summary = "参观预约列表")
    @GetMapping("/visits")
    public R<List<CrmVisit>> visits(@RequestParam(required = false)
                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        AuthGuard.requireTenant();
        return R.ok(leadService.visits(date));
    }

    @Operation(summary = "新建参观预约")
    @PostMapping("/visits")
    public R<Long> createVisit(@RequestBody CrmVisit request) {
        AuthGuard.requireTenant();
        return R.ok(leadService.createVisit(request));
    }

    @Operation(summary = "到店签到")
    @PostMapping("/visits/{id}/checkin")
    public R<Void> checkin(@PathVariable Long id) {
        AuthGuard.requireTenant();
        leadService.checkin(id);
        return R.ok();
    }
}
