package com.yzz.system.controller;

import com.yzz.common.api.R;
import com.yzz.common.audit.AuditLog;
import com.yzz.common.tenant.AuthGuard;
import com.yzz.system.dto.ConfigSaveRequest;
import com.yzz.system.service.ConfigService;
import com.yzz.system.vo.ConfigGroupVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 系统全局参数（T20）。金额与比例变更走二次确认，确认后落库并写审计。
 */
@Tag(name = "租户-系统参数")
@RestController
@RequestMapping("/api/tenant/configs")
public class ConfigController {

    private final ConfigService configService;

    public ConfigController(ConfigService configService) {
        this.configService = configService;
    }

    @Operation(summary = "分组参数列表")
    @GetMapping
    public R<List<ConfigGroupVO>> groups() {
        AuthGuard.requireTenant();
        return R.ok(configService.groups());
    }

    @Operation(summary = "读取单个参数")
    @GetMapping("/{key}")
    public R<String> value(@PathVariable String key) {
        AuthGuard.requireTenant();
        return R.ok(configService.getString(key, null));
    }

    @Operation(summary = "保存分组参数（金额与比例需二次确认）")
    @AuditLog(module = "系统参数", action = "保存参数")
    @PutMapping
    public R<List<String>> save(@Valid @RequestBody ConfigSaveRequest request) {
        AuthGuard.requireTenant();
        return R.ok(configService.saveGroup(request));
    }
}
