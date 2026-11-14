package com.yzz.platform.controller;

import com.yzz.common.api.R;
import com.yzz.common.audit.AuditLog;
import com.yzz.common.tenant.AuthGuard;
import com.yzz.platform.dto.PackageSaveRequest;
import com.yzz.platform.service.PlatformPackageService;
import com.yzz.platform.support.PlatformRoles;
import com.yzz.tenant.entity.PlatformPackage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "平台运营端-套餐与配额")
@RestController
@RequestMapping("/api/platform/packages")
public class PlatformPackageController {

    private final PlatformPackageService packageService;

    public PlatformPackageController(PlatformPackageService packageService) {
        this.packageService = packageService;
    }

    @Operation(summary = "套餐列表")
    @GetMapping
    public R<List<PlatformPackage>> list() {
        AuthGuard.requirePlatform();
        return R.ok(packageService.list());
    }

    @Operation(summary = "新增或修改套餐")
    @PostMapping
    @AuditLog(module = "套餐与配额", action = "保存套餐")
    public R<PlatformPackage> save(@Valid @RequestBody PackageSaveRequest request) {
        AuthGuard.requirePlatformRole(PlatformRoles.SUPER);
        return R.ok(packageService.save(request));
    }

    @Operation(summary = "删除套餐")
    @DeleteMapping("/{id}")
    @AuditLog(module = "套餐与配额", action = "删除套餐")
    public R<Void> delete(@PathVariable Long id) {
        AuthGuard.requirePlatformRole(PlatformRoles.SUPER);
        packageService.delete(id);
        return R.ok();
    }
}
