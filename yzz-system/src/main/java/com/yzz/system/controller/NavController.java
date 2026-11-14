package com.yzz.system.controller;

import com.yzz.common.api.R;
import com.yzz.common.audit.AuditLog;
import com.yzz.common.tenant.AuthGuard;
import com.yzz.system.dto.NavTreeRequest;
import com.yzz.system.service.NavService;
import com.yzz.system.vo.NavNodeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 多端菜单可视化（T22）：整树读取与整树保存，层级上限由服务端校验。
 */
@Tag(name = "租户-菜单可视化")
@RestController
@RequestMapping("/api/tenant/navs")
public class NavController {

    private final NavService navService;

    public NavController(NavService navService) {
        this.navService = navService;
    }

    @Operation(summary = "菜单树")
    @GetMapping
    public R<List<NavNodeVO>> tree(@RequestParam String endType) {
        AuthGuard.requireTenant();
        return R.ok(navService.tree(endType));
    }

    @Operation(summary = "整树保存")
    @AuditLog(module = "菜单可视化", action = "保存菜单")
    @PutMapping
    public R<List<NavNodeVO>> save(@Valid @RequestBody NavTreeRequest request) {
        AuthGuard.requireTenant();
        return R.ok(navService.saveTree(request));
    }
}
