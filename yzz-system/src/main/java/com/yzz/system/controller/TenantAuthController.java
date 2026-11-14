package com.yzz.system.controller;

import com.yzz.common.api.LoginVO;
import com.yzz.common.api.R;
import com.yzz.common.audit.AuditLog;
import com.yzz.common.tenant.AuthGuard;
import com.yzz.common.tenant.TenantContext;
import com.yzz.system.dto.TenantLoginRequest;
import com.yzz.system.service.TenantAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 租户后台登录入口。平台账号无法在此登录，平台账号也无法访问 /api/tenant 下的接口。
 */
@Tag(name = "租户后台-登录")
@RestController
@RequestMapping("/api/tenant/auth")
public class TenantAuthController {

    private final TenantAuthService authService;

    public TenantAuthController(TenantAuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "租户账号登录")
    @PostMapping("/login")
    @AuditLog(module = "认证", action = "租户登录", withArgs = false)
    public R<LoginVO> login(@Valid @RequestBody TenantLoginRequest request) {
        return R.ok(authService.login(request));
    }

    @Operation(summary = "当前登录账号")
    @GetMapping("/me")
    public R<String> me() {
        AuthGuard.requireTenant();
        return R.ok(TenantContext.getUsername());
    }
}
