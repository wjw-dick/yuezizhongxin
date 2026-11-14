package com.yzz.platform.controller;

import com.yzz.common.api.PageResult;
import com.yzz.common.api.R;
import com.yzz.common.tenant.AuthGuard;
import com.yzz.platform.dto.AuditQuery;
import com.yzz.platform.service.PlatformAuditService;
import com.yzz.platform.support.PlatformRoles;
import com.yzz.tenant.entity.PlatformAudit;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 平台审计日志。只读：无新增、编辑、删除接口。
 */
@Tag(name = "平台运营端-平台审计")
@RestController
@RequestMapping("/api/platform/audits")
public class PlatformAuditController {

    private final PlatformAuditService auditService;

    public PlatformAuditController(PlatformAuditService auditService) {
        this.auditService = auditService;
    }

    @Operation(summary = "审计日志列表（可按租户筛选）")
    @GetMapping
    public R<PageResult<PlatformAudit>> page(AuditQuery query) {
        AuthGuard.requirePlatformRole(PlatformRoles.SUPER, PlatformRoles.OPERATOR);
        return R.ok(auditService.page(query));
    }
}
