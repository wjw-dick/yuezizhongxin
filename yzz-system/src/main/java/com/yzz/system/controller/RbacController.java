package com.yzz.system.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yzz.common.api.R;
import com.yzz.common.tenant.AuthGuard;
import com.yzz.common.tenant.TenantContext;
import com.yzz.system.entity.SysEntry;
import com.yzz.system.entity.SysMenu;
import com.yzz.system.entity.SysRole;
import com.yzz.system.mapper.SysEntryMapper;
import com.yzz.system.service.RbacService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 角色权限、菜单与端入口（T50、T51）。
 */
@Tag(name = "租户-权限与入口")
@RestController
@RequestMapping("/api/tenant")
public class RbacController {

    private final RbacService rbacService;
    private final SysEntryMapper entryMapper;

    public RbacController(RbacService rbacService, SysEntryMapper entryMapper) {
        this.rbacService = rbacService;
        this.entryMapper = entryMapper;
    }

    @Operation(summary = "角色列表（含已授权菜单）")
    @GetMapping("/roles")
    public R<List<Map<String, Object>>> roles() {
        AuthGuard.requireTenant();
        return R.ok(rbacService.roles());
    }

    @Operation(summary = "保存角色")
    @PostMapping("/roles")
    public R<Long> saveRole(@RequestBody SysRole request) {
        AuthGuard.requireTenant();
        return R.ok(rbacService.saveRole(request));
    }

    @Operation(summary = "角色授权菜单")
    @PutMapping("/roles/{id}/menus")
    public R<Void> assignMenus(@PathVariable Long id, @RequestBody List<Long> menuIds) {
        AuthGuard.requireTenant();
        rbacService.assignMenus(id, menuIds);
        return R.ok();
    }

    @Operation(summary = "菜单与按钮列表")
    @GetMapping("/menus")
    public R<List<SysMenu>> menus() {
        AuthGuard.requireTenant();
        return R.ok(rbacService.menus());
    }

    @Operation(summary = "菜单树")
    @GetMapping("/menus/tree")
    public R<List<Map<String, Object>>> menuTree() {
        AuthGuard.requireTenant();
        return R.ok(rbacService.menuTree());
    }

    @Operation(summary = "当前登录人的菜单与按钮权限")
    @GetMapping("/auth/permissions")
    public R<Map<String, Object>> permissions() {
        AuthGuard.requireTenant();
        return R.ok(rbacService.myPermissions());
    }

    @Operation(summary = "端入口列表")
    @GetMapping("/entries")
    public R<List<SysEntry>> entries(@RequestParam(required = false) String entryType) {
        AuthGuard.requireTenant();
        return R.ok(entryMapper.selectList(Wrappers.<SysEntry>lambdaQuery()
                .eq(entryType != null && !entryType.isBlank(), SysEntry::getEntryType, entryType)
                .orderByDesc(SysEntry::getId)));
    }

    @Operation(summary = "保存端入口（带参码）")
    @PostMapping("/entries")
    public R<Long> saveEntry(@RequestBody SysEntry request) {
        AuthGuard.requireTenant();
        request.setTenantId(TenantContext.requireTenantId());
        if (request.getId() == null) {
            entryMapper.insert(request);
        } else {
            entryMapper.updateById(request);
        }
        return R.ok(request.getId());
    }

    @Operation(summary = "删除端入口")
    @DeleteMapping("/entries/{id}")
    public R<Void> deleteEntry(@PathVariable Long id) {
        AuthGuard.requireTenant();
        entryMapper.deleteById(id);
        return R.ok();
    }
}
