package com.yzz.system.controller;

import com.yzz.common.api.R;
import com.yzz.common.audit.AuditLog;
import com.yzz.common.tenant.AuthGuard;
import com.yzz.system.dto.I18nMessageRequest;
import com.yzz.system.dto.LanguageRequest;
import com.yzz.system.entity.SysI18nMessage;
import com.yzz.system.entity.SysLanguage;
import com.yzz.system.service.I18nService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 多语言配置（T21）。
 */
@Tag(name = "租户-多语言")
@RestController
@RequestMapping("/api/tenant/i18n")
public class I18nController {

    private final I18nService i18nService;

    public I18nController(I18nService i18nService) {
        this.i18nService = i18nService;
    }

    @Operation(summary = "语言列表")
    @GetMapping("/languages")
    public R<List<SysLanguage>> languages() {
        AuthGuard.requireTenant();
        return R.ok(i18nService.languages());
    }

    @Operation(summary = "新增语言")
    @AuditLog(module = "多语言", action = "新增语言")
    @PostMapping("/languages")
    public R<Void> createLanguage(@Valid @RequestBody LanguageRequest request) {
        AuthGuard.requireTenant();
        i18nService.saveLanguage(null, request);
        return R.ok();
    }

    @Operation(summary = "修改语言")
    @AuditLog(module = "多语言", action = "修改语言")
    @PutMapping("/languages/{id}")
    public R<Void> updateLanguage(@PathVariable Long id,
                                  @Valid @RequestBody LanguageRequest request) {
        AuthGuard.requireTenant();
        i18nService.saveLanguage(id, request);
        return R.ok();
    }

    @Operation(summary = "设为默认语言")
    @AuditLog(module = "多语言", action = "切换默认语言")
    @PutMapping("/languages/{id}/default")
    public R<Void> setDefault(@PathVariable Long id) {
        AuthGuard.requireTenant();
        i18nService.setDefault(id);
        return R.ok();
    }

    @Operation(summary = "删除语言")
    @AuditLog(module = "多语言", action = "删除语言")
    @DeleteMapping("/languages/{id}")
    public R<Void> deleteLanguage(@PathVariable Long id) {
        AuthGuard.requireTenant();
        i18nService.deleteLanguage(id);
        return R.ok();
    }

    @Operation(summary = "语言包词条")
    @GetMapping("/messages")
    public R<List<SysI18nMessage>> messages(@RequestParam String langCode) {
        AuthGuard.requireTenant();
        return R.ok(i18nService.messages(langCode));
    }

    @Operation(summary = "批量保存语言包")
    @AuditLog(module = "多语言", action = "保存语言包")
    @PutMapping("/messages")
    public R<Integer> saveMessages(@Valid @RequestBody I18nMessageRequest request) {
        AuthGuard.requireTenant();
        return R.ok(i18nService.saveMessages(request));
    }

    @Operation(summary = "语言包（缺失词条回落默认语言）")
    @GetMapping("/pack/{langCode}")
    public R<Map<String, String>> pack(@PathVariable String langCode) {
        AuthGuard.requireTenant();
        return R.ok(i18nService.pack(langCode));
    }
}
