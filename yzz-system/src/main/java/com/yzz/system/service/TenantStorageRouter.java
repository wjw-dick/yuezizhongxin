package com.yzz.system.service;

import com.yzz.common.storage.StoredFile;
import com.yzz.common.storage.StorageService;
import com.yzz.common.storage.StorageSettings;
import com.yzz.common.tenant.TenantContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 租户级存储路由（T23/T24）。
 *
 * <p>业务代码注入 {@link StorageService} 即得到本实现：按当前租户的存储配置选择驱动，
 * 未配置或驱动不可用时回退本地磁盘，保证上传链路不中断。配置缓存 60 秒，
 * 后台切换驱动后最多一分钟后全端生效。</p>
 */
@Service
@Primary
public class TenantStorageRouter implements StorageService {

    private static final long CACHE_MILLIS = 60_000L;

    private final StorageService local;
    private final StorageConfigService configService;
    private final Map<Long, Cached> cache = new ConcurrentHashMap<>();

    public TenantStorageRouter(@Qualifier("localStorageService") StorageService local,
                               StorageConfigService configService) {
        this.local = local;
        this.configService = configService;
    }

    @Override
    public StoredFile upload(String tenantCode, String dir, String filename,
                             InputStream inputStream, long size, String contentType) {
        return resolve().upload(tenantCode, dir, filename, inputStream, size, contentType);
    }

    @Override
    public Resource download(String tenantCode, String objectKey) {
        return resolve().download(tenantCode, objectKey);
    }

    @Override
    public String signedUrl(String tenantCode, String objectKey, Duration ttl) {
        return resolve().signedUrl(tenantCode, objectKey, ttl);
    }

    @Override
    public void delete(String tenantCode, String objectKey) {
        resolve().delete(tenantCode, objectKey);
    }

    @Override
    public String driver() {
        return resolve().driver();
    }

    @Override
    public void testConnection(String tenantCode) {
        resolve().testConnection(tenantCode);
    }

    /** 上传后落库文件台账等场景需要知道当前租户实际使用的驱动名。 */
    public String currentDriver() {
        return resolve().driver();
    }

    private StorageService resolve() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            return local;
        }
        Cached cached = cache.get(tenantId);
        if (cached != null && cached.expireAt > System.currentTimeMillis()) {
            return cached.service;
        }
        StorageService service;
        try {
            StorageSettings settings = configService.settingsOf(tenantId);
            service = "local".equals(settings.getDriver())
                    ? local : configService.create(settings);
        } catch (RuntimeException e) {
            // 配置损坏时不阻断上传，降级本地并留待后台修正
            service = local;
        }
        cache.put(tenantId, new Cached(service, System.currentTimeMillis() + CACHE_MILLIS));
        return service;
    }

    public void evict(Long tenantId) {
        cache.remove(tenantId);
    }

    private record Cached(StorageService service, long expireAt) {
    }
}
