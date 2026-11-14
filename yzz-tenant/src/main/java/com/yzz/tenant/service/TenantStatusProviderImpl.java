package com.yzz.tenant.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yzz.common.guard.CacheStore;
import com.yzz.common.tenant.TenantState;
import com.yzz.common.tenant.TenantStatusProvider;
import com.yzz.tenant.entity.PlatformTenant;
import com.yzz.tenant.mapper.PlatformTenantMapper;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 租户状态查询，带 60 秒本地/分布式缓存。
 *
 * <p>缓存时长即「停用生效延迟」上限：平台停用后最迟 60 秒内全部请求被拒，
 * 停用操作会主动清理该键，实际生效时间为秒级。</p>
 */
@Service
public class TenantStatusProviderImpl implements TenantStatusProvider {

    private static final Duration TTL = Duration.ofSeconds(60);
    private static final String KEY_PREFIX = "tenant:state:";
    private static final String ID_KEY_PREFIX = "tenant:id:";

    private final PlatformTenantMapper tenantMapper;
    private final CacheStore cacheStore;

    public TenantStatusProviderImpl(PlatformTenantMapper tenantMapper, CacheStore cacheStore) {
        this.tenantMapper = tenantMapper;
        this.cacheStore = cacheStore;
    }

    @Override
    public TenantState stateOf(String tenantCode) {
        if (tenantCode == null || tenantCode.isBlank()) {
            return TenantState.UNKNOWN;
        }
        String key = KEY_PREFIX + tenantCode;
        String cached = cacheStore.get(key);
        if (cached != null) {
            return TenantState.of(cached);
        }
        PlatformTenant tenant = tenantMapper.selectOne(Wrappers.<PlatformTenant>lambdaQuery()
                .eq(PlatformTenant::getTenantCode, tenantCode)
                .last("limit 1"));
        if (tenant == null) {
            return TenantState.UNKNOWN;
        }
        cacheStore.put(key, tenant.getStatus(), TTL);
        return TenantState.of(tenant.getStatus());
    }

    @Override
    public Long idOf(String tenantCode) {
        if (tenantCode == null || tenantCode.isBlank()) {
            return null;
        }
        String key = ID_KEY_PREFIX + tenantCode;
        String cached = cacheStore.get(key);
        if (cached != null) {
            return Long.valueOf(cached);
        }
        PlatformTenant tenant = tenantMapper.selectOne(Wrappers.<PlatformTenant>lambdaQuery()
                .eq(PlatformTenant::getTenantCode, tenantCode)
                .last("limit 1"));
        if (tenant == null) {
            return null;
        }
        cacheStore.put(key, String.valueOf(tenant.getId()), TTL);
        return tenant.getId();
    }

    /** 状态变更后立即失效缓存，保证停用、续期秒级生效。 */
    public void evict(String tenantCode) {
        cacheStore.delete(KEY_PREFIX + tenantCode);
        cacheStore.delete(ID_KEY_PREFIX + tenantCode);
    }
}
