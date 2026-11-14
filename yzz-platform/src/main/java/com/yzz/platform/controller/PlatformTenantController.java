package com.yzz.platform.controller;

import com.yzz.common.api.PageResult;
import com.yzz.common.api.R;
import com.yzz.common.audit.AuditLog;
import com.yzz.common.tenant.AuthGuard;
import com.yzz.platform.dto.QuotaAdjustRequest;
import com.yzz.platform.dto.TenantCreateRequest;
import com.yzz.platform.dto.TenantQuery;
import com.yzz.platform.dto.TenantRenewRequest;
import com.yzz.platform.service.PlatformTenantService;
import com.yzz.platform.support.PlatformRoles;
import com.yzz.platform.vo.TenantCreateResult;
import com.yzz.platform.vo.TenantVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 租户管理。平台端只返回汇总与状态，不返回租户订单、会员、护理记录等业务明细。
 */
@Tag(name = "平台运营端-租户管理")
@RestController
@RequestMapping("/api/platform/tenants")
public class PlatformTenantController {

    private final PlatformTenantService tenantService;

    public PlatformTenantController(PlatformTenantService tenantService) {
        this.tenantService = tenantService;
    }

    @Operation(summary = "租户列表")
    @GetMapping
    public R<PageResult<TenantVO>> page(TenantQuery query) {
        AuthGuard.requirePlatform();
        return R.ok(tenantService.page(query));
    }

    @Operation(summary = "租户详情")
    @GetMapping("/{id}")
    public R<TenantVO> detail(@PathVariable Long id) {
        AuthGuard.requirePlatform();
        return R.ok(tenantService.detail(id));
    }

    @Operation(summary = "开通租户（返回一次性初始口令）")
    @PostMapping
    @AuditLog(module = "租户管理", action = "开通租户", withArgs = false)
    public R<TenantCreateResult> create(@Valid @RequestBody TenantCreateRequest request) {
        AuthGuard.requirePlatformRole(PlatformRoles.SUPER, PlatformRoles.OPERATOR);
        return R.ok(tenantService.create(request));
    }

    @Operation(summary = "停用租户")
    @PostMapping("/{id}/disable")
    @AuditLog(module = "租户管理", action = "停用租户")
    public R<Void> disable(@PathVariable Long id) {
        AuthGuard.requirePlatformRole(PlatformRoles.SUPER, PlatformRoles.OPERATOR);
        tenantService.disable(id);
        return R.ok();
    }

    @Operation(summary = "恢复租户")
    @PostMapping("/{id}/resume")
    @AuditLog(module = "租户管理", action = "恢复租户")
    public R<Void> resume(@PathVariable Long id) {
        AuthGuard.requirePlatformRole(PlatformRoles.SUPER, PlatformRoles.OPERATOR);
        tenantService.resume(id);
        return R.ok();
    }

    @Operation(summary = "租户续期")
    @PostMapping("/{id}/renew")
    @AuditLog(module = "租户管理", action = "租户续期")
    public R<Void> renew(@PathVariable Long id, @Valid @RequestBody TenantRenewRequest request) {
        AuthGuard.requirePlatformRole(PlatformRoles.SUPER, PlatformRoles.OPERATOR);
        tenantService.renew(id, request.days());
        return R.ok();
    }

    @Operation(summary = "删除租户（逻辑删除，数据归档）")
    @DeleteMapping("/{id}")
    @AuditLog(module = "租户管理", action = "删除租户")
    public R<Void> remove(@PathVariable Long id) {
        AuthGuard.requirePlatformRole(PlatformRoles.SUPER);
        tenantService.remove(id);
        return R.ok();
    }

    @Operation(summary = "调整单租户配额（需填原因并留痕）")
    @PutMapping("/{id}/quota")
    @AuditLog(module = "租户管理", action = "调整配额")
    public R<Void> adjustQuota(@PathVariable Long id, @Valid @RequestBody QuotaAdjustRequest request) {
        AuthGuard.requirePlatformRole(PlatformRoles.SUPER);
        tenantService.adjustQuota(id, request);
        return R.ok();
    }
}
