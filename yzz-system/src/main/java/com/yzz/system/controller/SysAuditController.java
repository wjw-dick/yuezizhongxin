package com.yzz.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yzz.common.api.PageResult;
import com.yzz.common.api.R;
import com.yzz.common.tenant.AuthGuard;
import com.yzz.system.dto.SysAuditQuery;
import com.yzz.system.entity.SysAuditLog;
import com.yzz.system.mapper.SysAuditLogMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 租户审计日志。只读：只提供查询，不提供新增、修改、删除（保留 ≥180 天由运维备份策略保证）。
 */
@Tag(name = "租户后台-审计日志")
@RestController
@RequestMapping("/api/tenant/audits")
public class SysAuditController {

    private final SysAuditLogMapper auditLogMapper;

    public SysAuditController(SysAuditLogMapper auditLogMapper) {
        this.auditLogMapper = auditLogMapper;
    }

    @Operation(summary = "审计日志列表（自动限定当前租户）")
    @GetMapping
    public R<PageResult<SysAuditLog>> page(SysAuditQuery query) {
        AuthGuard.requireTenant();
        var wrapper = Wrappers.<SysAuditLog>lambdaQuery()
                .eq(query.getModule() != null && !query.getModule().isBlank(),
                        SysAuditLog::getModule, query.getModule())
                .eq(query.getAction() != null && !query.getAction().isBlank(),
                        SysAuditLog::getAction, query.getAction())
                .like(query.getOperatorName() != null && !query.getOperatorName().isBlank(),
                        SysAuditLog::getOperatorName, query.getOperatorName())
                .ge(query.getStartTime() != null, SysAuditLog::getCreatedTime, query.getStartTime())
                .le(query.getEndTime() != null, SysAuditLog::getCreatedTime, query.getEndTime())
                .orderByDesc(SysAuditLog::getCreatedTime);
        IPage<SysAuditLog> page = auditLogMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        return R.ok(PageResult.of(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize()));
    }
}
