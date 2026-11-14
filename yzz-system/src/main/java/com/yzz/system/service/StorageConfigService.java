package com.yzz.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yzz.common.api.BusinessException;
import com.yzz.common.api.ErrorCode;
import com.yzz.common.guard.CacheStore;
import com.yzz.common.storage.LocalStorageService;
import com.yzz.common.storage.S3CompatibleStorageService;
import com.yzz.common.storage.StorageProperties;
import com.yzz.common.storage.StorageService;
import com.yzz.common.storage.StorageSettings;
import com.yzz.common.tenant.TenantContext;
import com.yzz.common.util.SecretCipher;
import com.yzz.system.dto.StorageConfigRequest;
import com.yzz.system.dto.StorageTestRequest;
import com.yzz.system.entity.SysStorageConfig;
import com.yzz.system.mapper.SysStorageConfigMapper;
import com.yzz.system.vo.StorageConfigVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 对象存储配置与切换（T23/T24）。
 *
 * <p>5 张驱动卡片：本地磁盘、AWS S3、MinIO、阿里云 OSS、腾讯云 COS；
 * 后四者共用 S3 兼容实现。密钥加密落库、只回掩码；连接测试通过才允许保存，
 * 切换驱动需二次确认。</p>
 */
@Service
public class StorageConfigService {

    private static final Logger log = LoggerFactory.getLogger(StorageConfigService.class);
    private static final String TEST_PREFIX = "storage:test:";
    private static final Duration TEST_TTL = Duration.ofMinutes(10);
    public static final List<String> DRIVERS = List.of("local", "s3", "minio", "oss", "cos");

    private final SysStorageConfigMapper configMapper;
    private final SecretCipher secretCipher;
    private final ObjectMapper objectMapper;
    private final CacheStore cacheStore;
    private final StorageProperties properties;

    public StorageConfigService(SysStorageConfigMapper configMapper, SecretCipher secretCipher,
                                ObjectMapper objectMapper, CacheStore cacheStore,
                                StorageProperties properties) {
        this.configMapper = configMapper;
        this.secretCipher = secretCipher;
        this.objectMapper = objectMapper;
        this.cacheStore = cacheStore;
        this.properties = properties;
    }

    public StorageConfigVO current() {
        SysStorageConfig row = findRow();
        if (row == null) {
            return new StorageConfigVO(properties.getDriver(), null, null, null,
                    null, null, true, properties.getPublicBaseUrl(), false, null);
        }
        StorageSettings settings = parse(row.getConfigJson());
        return new StorageConfigVO(row.getDriver(), settings.getEndpoint(), settings.getRegion(),
                settings.getBucket(), mask(settings.getAccessKey()), mask(settings.getSecretKey()),
                settings.isPathStyle(), settings.getPublicBaseUrl(),
                row.getTested() != null && row.getTested() == 1, row.getUpdatedTime());
    }

    /** 未保存的配置先测通再保存。 */
    public String test(StorageTestRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        StorageSettings settings = build(request.driver(), request.endpoint(), request.region(),
                request.bucket(), request.accessKey(), request.secretKey(), request.pathStyle(),
                null, findRow());
        StorageService service = create(settings);
        String tenantCode = TenantContext.getTenantCode();
        try {
            service.testConnection(tenantCode);
        } catch (BusinessException e) {
            throw BusinessException.of(ErrorCode.STORAGE_ERROR, "连接测试未通过：" + e.getMessage());
        }
        cacheStore.put(TEST_PREFIX + tenantId, fingerprint(settings), TEST_TTL);
        return "连接测试通过（驱动：" + settings.getDriver() + "）";
    }

    @Transactional
    public StorageConfigVO save(StorageConfigRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        if (!DRIVERS.contains(request.driver())) {
            throw BusinessException.of(ErrorCode.BAD_REQUEST, "不支持的存储驱动：" + request.driver());
        }
        SysStorageConfig row = findRow();
        String previousDriver = row == null ? properties.getDriver() : row.getDriver();
        if (!previousDriver.equals(request.driver()) && !request.confirmSwitch()) {
            throw BusinessException.of(ErrorCode.CONFLICT,
                    "从「" + previousDriver + "」切换到「" + request.driver() + "」后，历史文件的访问路径不变，"
                            + "但新文件将写入新驱动，请确认后重试");
        }
        StorageSettings settings = build(request.driver(), request.endpoint(), request.region(),
                request.bucket(), request.accessKey(), request.secretKey(), request.pathStyle(),
                request.publicBaseUrl(), row);

        if (!"local".equals(request.driver())) {
            String tested = cacheStore.get(TEST_PREFIX + tenantId);
            if (tested == null || !tested.equals(fingerprint(settings))) {
                throw BusinessException.of(ErrorCode.BAD_REQUEST, "请先完成连接测试，测试通过后才可保存");
            }
        }

        String json;
        try {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("endpoint", settings.getEndpoint());
            map.put("region", settings.getRegion());
            map.put("bucket", settings.getBucket());
            map.put("accessKey", settings.getAccessKey());
            map.put("secretKey", secretCipher.encrypt(settings.getSecretKey()));
            map.put("pathStyle", settings.isPathStyle());
            map.put("publicBaseUrl", settings.getPublicBaseUrl());
            json = objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            throw BusinessException.of(ErrorCode.INTERNAL_ERROR, "存储配置序列化失败");
        }

        if (row == null) {
            row = new SysStorageConfig();
            row.setTenantId(tenantId);
            row.setDriver(request.driver());
            row.setConfigJson(json);
            row.setTested("local".equals(request.driver()) ? 1 : 1);
            configMapper.insert(row);
        } else {
            row.setDriver(request.driver());
            row.setConfigJson(json);
            row.setTested(1);
            configMapper.updateById(row);
        }
        cacheStore.delete(TEST_PREFIX + tenantId);
        log.info("对象存储配置已更新 tenantId={} driver={}", tenantId, request.driver());
        return current();
    }

    /** 供租户存储路由读取当前生效配置。 */
    public StorageSettings settingsOf(Long tenantId) {
        SysStorageConfig row = configMapper.selectOne(Wrappers.<SysStorageConfig>lambdaQuery()
                .eq(SysStorageConfig::getTenantId, tenantId).last("limit 1"));
        if (row == null) {
            StorageSettings fallback = new StorageSettings();
            fallback.setDriver(properties.getDriver());
            fallback.setLocalRoot(properties.getLocalRoot());
            fallback.setPublicBaseUrl(properties.getPublicBaseUrl());
            return fallback;
        }
        StorageSettings settings = parse(row.getConfigJson());
        settings.setDriver(row.getDriver());
        if (settings.getLocalRoot() == null) {
            settings.setLocalRoot(properties.getLocalRoot());
        }
        if (settings.getPublicBaseUrl() == null || settings.getPublicBaseUrl().isBlank()) {
            settings.setPublicBaseUrl(properties.getPublicBaseUrl());
        }
        return settings;
    }

    public StorageService create(StorageSettings settings) {
        if ("local".equals(settings.getDriver())) {
            StorageProperties local = new StorageProperties();
            local.setDriver("local");
            local.setLocalRoot(settings.getLocalRoot() == null
                    ? properties.getLocalRoot() : settings.getLocalRoot());
            local.setPublicBaseUrl(settings.getPublicBaseUrl() == null
                    ? properties.getPublicBaseUrl() : settings.getPublicBaseUrl());
            local.setMaxSizeMb(properties.getMaxSizeMb());
            return new LocalStorageService(local);
        }
        return new S3CompatibleStorageService(settings, properties.getMaxSizeMb());
    }

    private SysStorageConfig findRow() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            return null;
        }
        return configMapper.selectOne(Wrappers.<SysStorageConfig>lambdaQuery()
                .eq(SysStorageConfig::getTenantId, tenantId).last("limit 1"));
    }

    private StorageSettings build(String driver, String endpoint, String region, String bucket,
                                  String accessKey, String secretKey, Boolean pathStyle,
                                  String publicBaseUrl, SysStorageConfig existing) {
        StorageSettings settings = new StorageSettings();
        settings.setDriver(driver);
        settings.setEndpoint(endpoint);
        settings.setRegion(region == null || region.isBlank() ? "us-east-1" : region);
        settings.setBucket(bucket);
        settings.setAccessKey(accessKey);
        settings.setPathStyle(pathStyle == null || pathStyle);
        if (publicBaseUrl != null && !publicBaseUrl.isBlank()) {
            settings.setPublicBaseUrl(publicBaseUrl);
        }
        settings.setLocalRoot(properties.getLocalRoot());

        String secret = secretKey;
        if ((secret == null || secret.isBlank()) && existing != null) {
            StorageSettings old = parse(existing.getConfigJson());
            secret = old.getSecretKey();
            if (settings.getAccessKey() == null || settings.getAccessKey().isBlank()) {
                settings.setAccessKey(old.getAccessKey());
            }
            if (settings.getEndpoint() == null || settings.getEndpoint().isBlank()) {
                settings.setEndpoint(old.getEndpoint());
            }
            if (settings.getBucket() == null || settings.getBucket().isBlank()) {
                settings.setBucket(old.getBucket());
            }
        }
        settings.setSecretKey(secret);
        if (!"local".equals(driver)) {
            if (isBlank(settings.getEndpoint()) || isBlank(settings.getBucket())) {
                throw BusinessException.of(ErrorCode.BAD_REQUEST, "服务地址与 Bucket 不能为空");
            }
        }
        return settings;
    }

    private StorageSettings parse(String json) {
        StorageSettings settings = new StorageSettings();
        if (json == null || json.isBlank()) {
            return settings;
        }
        try {
            Map<String, Object> map = objectMapper.readValue(json,
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
                    });
            settings.setEndpoint(str(map.get("endpoint")));
            settings.setRegion(str(map.get("region")));
            settings.setBucket(str(map.get("bucket")));
            settings.setAccessKey(str(map.get("accessKey")));
            String encrypted = str(map.get("secretKey"));
            settings.setSecretKey(encrypted == null || encrypted.isBlank()
                    ? null : secretCipher.decrypt(encrypted));
            Object pathStyle = map.get("pathStyle");
            settings.setPathStyle(pathStyle == null || Boolean.parseBoolean(pathStyle.toString()));
            settings.setPublicBaseUrl(str(map.get("publicBaseUrl")));
        } catch (Exception e) {
            log.warn("存储配置解析失败，回退默认驱动", e);
        }
        return settings;
    }

    private String fingerprint(StorageSettings settings) {
        String raw = settings.getDriver() + "|" + settings.getEndpoint() + "|"
                + settings.getRegion() + "|" + settings.getBucket() + "|"
                + settings.getAccessKey() + "|" + settings.getSecretKey() + "|"
                + settings.isPathStyle();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw BusinessException.of(ErrorCode.INTERNAL_ERROR, "计算配置指纹失败");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    static String mask(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if (value.length() <= 8) {
            return "****";
        }
        return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
    }
}
