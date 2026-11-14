package com.yzz.content.controller;

import com.yzz.common.api.R;
import com.yzz.common.tenant.AuthGuard;
import com.yzz.content.entity.CmsContent;
import com.yzz.content.entity.CmsContentI18n;
import com.yzz.content.service.CmsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
 * 内容 CMS 维护与发布（T48）。
 */
@Tag(name = "租户-内容 CMS")
@RestController
@RequestMapping("/api/tenant/cms")
public class CmsController {

    private final CmsService cmsService;

    public CmsController(CmsService cmsService) {
        this.cmsService = cmsService;
    }

    @Operation(summary = "内容列表")
    @GetMapping
    public R<List<CmsContent>> list(@RequestParam(required = false) String contentType,
                                    @RequestParam(required = false) String status,
                                    @RequestParam(required = false) String keyword) {
        AuthGuard.requireTenant();
        return R.ok(cmsService.list(contentType, status, keyword));
    }

    @Operation(summary = "内容详情（含多语言）")
    @GetMapping("/{id}")
    public R<Map<String, Object>> detail(@PathVariable Long id) {
        AuthGuard.requireTenant();
        return R.ok(cmsService.detail(id));
    }

    @Operation(summary = "保存内容")
    @PostMapping
    public R<Long> save(@RequestBody CmsContent request) {
        AuthGuard.requireTenant();
        return R.ok(cmsService.save(request));
    }

    @Operation(summary = "发布内容")
    @PostMapping("/{id}/publish")
    public R<Void> publish(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        AuthGuard.requireTenant();
        cmsService.publish(id, body == null ? null : body.get("applyEnds"));
        return R.ok();
    }

    @Operation(summary = "下线内容")
    @PostMapping("/{id}/offline")
    public R<Void> offline(@PathVariable Long id) {
        AuthGuard.requireTenant();
        cmsService.offline(id);
        return R.ok();
    }

    @Operation(summary = "保存多语言内容")
    @PutMapping("/{id}/i18n")
    public R<Void> saveI18n(@PathVariable Long id, @RequestBody List<CmsContentI18n> rows) {
        AuthGuard.requireTenant();
        cmsService.saveI18n(id, rows);
        return R.ok();
    }
}
