package com.yzz.platform.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yzz.common.api.PageResult;
import com.yzz.platform.dto.AuditQuery;
import com.yzz.tenant.entity.PlatformAudit;
import com.yzz.tenant.mapper.PlatformAuditMapper;
import org.springframework.stereotype.Service;

/**
 * 平台审计查询。只读，不提供任何写入/删除接口。
 */
@Service
public class PlatformAuditService {

    private final PlatformAuditMapper auditMapper;

    public PlatformAuditService(PlatformAuditMapper auditMapper) {
        this.auditMapper = auditMapper;
    }

    public PageResult<PlatformAudit> page(AuditQuery query) {
        var wrapper = Wrappers.<PlatformAudit>lambdaQuery()
                .eq(query.getTenantId() != null, PlatformAudit::getTenantId, query.getTenantId())
                .eq(query.getModule() != null && !query.getModule().isBlank(),
                        PlatformAudit::getModule, query.getModule())
                .ge(query.getStartTime() != null, PlatformAudit::getCreatedTime, query.getStartTime())
                .le(query.getEndTime() != null, PlatformAudit::getCreatedTime, query.getEndTime())
                .orderByDesc(PlatformAudit::getCreatedTime);
        IPage<PlatformAudit> page = auditMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        return PageResult.of(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }
}
