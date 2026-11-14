package com.yzz.platform.controller;

import com.yzz.common.api.R;
import com.yzz.common.audit.AuditLog;
import com.yzz.common.tenant.AuthGuard;
import com.yzz.common.tenant.TenantContext;
import com.yzz.platform.dto.PlatformLoginRequest;
import com.yzz.platform.service.PlatformAuthService;
import com.yzz.platform.service.PlatformMenuService;
import com.yzz.platform.support.PlatformRoles;
import com.yzz.common.api.LoginVO;
import com.yzz.platform.vo.MenuNodeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 平台运营端登录入口。与租户后台登录入口物理分离（/api/platform/auth 与 /api/tenant/auth）。
 */
@Tag(name = "平台运营端-登录")
@RestController
@RequestMapping("/api/platform/auth")
public class PlatformAuthController {

    private final PlatformAuthService authService;
    private final PlatformMenuService menuService;

    public PlatformAuthController(PlatformAuthService authService, PlatformMenuService menuService) {
        this.authService = authService;
        this.menuService = menuService;
    }

    @Operation(summary = "平台账号登录")
    @PostMapping("/login")
    public R<LoginVO> login(@Valid @RequestBody PlatformLoginRequest request) {
        return R.ok(authService.login(request));
    }

    @Operation(summary = "当前平台账号的菜单树")
    @GetMapping("/menus")
    public R<List<MenuNodeVO>> menus() {
        AuthGuard.requirePlatform();
        return R.ok(menuService.treeOf(TenantContext.getRoleCode()));
    }

    @Operation(summary = "当前平台账号信息")
    @GetMapping("/me")
    public R<LoginVO> me() {
        AuthGuard.requirePlatform();
        return R.ok(new LoginVO(null, TenantContext.getUsername(), null,
                TenantContext.getRoleCode(), PlatformRoles.nameOf(TenantContext.getRoleCode()), true, null));
    }
}
