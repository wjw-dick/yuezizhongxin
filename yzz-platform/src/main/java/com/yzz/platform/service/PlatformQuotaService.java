package com.yzz.platform.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yzz.common.api.BusinessException;
import com.yzz.common.api.ErrorCode;
import com.yzz.tenant.entity.PlatformTenant;
import com.yzz.tenant.entity.PlatformTenantUsage;
import com.yzz.tenant.mapper.PlatformTenantMapper;
import com.yzz.tenant.mapper.PlatformTenantUsageMapper;
import org.springframework.stereotype.Service;

/**
 * 五类配额校验：席位、公众号数、存储容量、短信条数、接口调用量。
 *
 * <p>超限一律显式拒绝并给出升级引导，禁止静默放行。调用点应在写操作前执行。</p>
 */
@Service
public class PlatformQuotaService {

    public enum Quota {
        SEAT("席位"),
        MP("公众号数"),
        STORAGE("存储容量（GB）"),
        SMS("短信条数"),
        API("接口调用量");

        private final String label;

        Quota(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    private final PlatformTenantMapper tenantMapper;
    private final PlatformTenantUsageMapper usageMapper;

    public PlatformQuotaService(PlatformTenantMapper tenantMapper,
                                PlatformTenantUsageMapper usageMapper) {
        this.tenantMapper = tenantMapper;
        this.usageMapper = usageMapper;
    }

    public void check(Long tenantId, Quota quota, int delta) {
        PlatformTenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null) {
            throw BusinessException.of(ErrorCode.TENANT_NOT_FOUND);
        }
        PlatformTenantUsage usage = usageMapper.selectOne(Wrappers.<PlatformTenantUsage>lambdaQuery()
                .eq(PlatformTenantUsage::getTenantId, tenantId)
                .orderByDesc(PlatformTenantUsage::getStatDate)
                .last("limit 1"));
        int limit = limitOf(tenant, quota);
        int used = usedOf(usage, quota);
        if (used + delta > limit) {
            throw BusinessException.of(ErrorCode.QUOTA_EXCEEDED,
                    quota.getLabel() + "已用 " + used + "，上限 " + limit
                            + "，本次需要 " + delta + " 个额度。请联系平台运营方升级套餐或调整配额");
        }
    }

    private int limitOf(PlatformTenant tenant, Quota quota) {
        return switch (quota) {
            case SEAT -> nullToZero(tenant.getSeatLimit());
            case MP -> nullToZero(tenant.getMpLimit());
            case STORAGE -> nullToZero(tenant.getStorageLimitGb());
            case SMS -> nullToZero(tenant.getSmsLimit());
            case API -> nullToZero(tenant.getApiLimit());
        };
    }

    private int usedOf(PlatformTenantUsage usage, Quota quota) {
        if (usage == null) {
            return 0;
        }
        return switch (quota) {
            case SEAT -> nullToZero(usage.getUsedSeat());
            case MP -> nullToZero(usage.getUsedMp());
            case STORAGE -> nullToZero(usage.getUsedStorageGb());
            case SMS -> nullToZero(usage.getUsedSms());
            case API -> nullToZero(usage.getUsedApi());
        };
    }

    private int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }
}
