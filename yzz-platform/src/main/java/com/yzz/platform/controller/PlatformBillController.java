package com.yzz.platform.controller;

import com.yzz.common.api.PageResult;
import com.yzz.common.api.R;
import com.yzz.common.audit.AuditLog;
import com.yzz.common.tenant.AuthGuard;
import com.yzz.platform.dto.BillGenerateRequest;
import com.yzz.platform.dto.BillQuery;
import com.yzz.platform.service.PlatformBillService;
import com.yzz.platform.support.PlatformRoles;
import com.yzz.tenant.entity.PlatformBill;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "平台运营端-账单与计费")
@RestController
@RequestMapping("/api/platform/bills")
public class PlatformBillController {

    private final PlatformBillService billService;

    public PlatformBillController(PlatformBillService billService) {
        this.billService = billService;
    }

    @Operation(summary = "账单列表")
    @GetMapping
    public R<PageResult<PlatformBill>> page(BillQuery query) {
        AuthGuard.requirePlatform();
        return R.ok(billService.page(query));
    }

    @Operation(summary = "按账期生成账单（按席位用量）")
    @PostMapping("/generate")
    @AuditLog(module = "账单与计费", action = "生成账单")
    public R<Integer> generate(@Valid @RequestBody BillGenerateRequest request) {
        AuthGuard.requirePlatformRole(PlatformRoles.SUPER, PlatformRoles.FINANCE);
        return R.ok(billService.generate(request.billPeriod()));
    }

    @Operation(summary = "标记账单已支付")
    @PostMapping("/{id}/pay")
    @AuditLog(module = "账单与计费", action = "标记已支付")
    public R<Void> markPaid(@PathVariable Long id) {
        AuthGuard.requirePlatformRole(PlatformRoles.SUPER, PlatformRoles.FINANCE);
        billService.markPaid(id);
        return R.ok();
    }

    @Operation(summary = "逾期扫描")
    @PostMapping("/mark-overdue")
    @AuditLog(module = "账单与计费", action = "逾期扫描")
    public R<Integer> markOverdue() {
        AuthGuard.requirePlatformRole(PlatformRoles.SUPER, PlatformRoles.FINANCE);
        return R.ok(billService.markOverdue());
    }
}
