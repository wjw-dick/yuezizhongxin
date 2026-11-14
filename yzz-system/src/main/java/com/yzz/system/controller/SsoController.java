package com.yzz.system.controller;

import com.yzz.common.api.R;
import com.yzz.common.audit.AuditLog;
import com.yzz.common.tenant.AuthGuard;
import com.yzz.system.dto.SsoConfigRequest;
import com.yzz.system.service.SsoService;
import com.yzz.system.vo.SsoConfigVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * SSO 配置（T28）：四种协议、根地址自动拼路径、Secret 掩码、故障降级。
 */
@Tag(name = "租户-SSO")
@RestController
@RequestMapping("/api/tenant/sso-config")
public class SsoController {

    private final SsoService ssoService;

    public SsoController(SsoService ssoService) {
        this.ssoService = ssoService;
    }

    @Operation(summary = "当前 SSO 配置")
    @GetMapping
    public R<SsoConfigVO> current() {
        AuthGuard.requireTenant();
        return R.ok(ssoService.current());
    }

    @Operation(summary = "保存 SSO 配置")
    @AuditLog(module = "SSO", action = "保存配置", withArgs = false)
    @PutMapping
    public R<SsoConfigVO> save(@Valid @RequestBody SsoConfigRequest request) {
        AuthGuard.requireTenant();
        return R.ok(ssoService.save(request));
    }
}
