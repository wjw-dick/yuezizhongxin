package com.yzz.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yzz.common.api.BusinessException;
import com.yzz.common.api.ErrorCode;
import com.yzz.common.guard.CacheStore;
import com.yzz.common.tenant.TenantContext;
import com.yzz.system.dto.ConfigSaveRequest;
import com.yzz.system.entity.SysConfig;
import com.yzz.system.mapper.SysConfigMapper;
import com.yzz.system.vo.ConfigGroupVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统全局参数（T20）。
 *
 * <p>六个分组：站点基础、门店信息、订单规则、护理规则、营销规则、消息。
 * 保存即时生效（写库 + 失效缓存）；金额与比例类参数变更需二次确认并写审计。</p>
 */
@Service
public class ConfigService {

    private static final Logger log = LoggerFactory.getLogger(ConfigService.class);
    private static final String CACHE_PREFIX = "sys:config:";
    private static final Duration TTL = Duration.ofMinutes(5);
    private static final List<String> SENSITIVE_HINTS =
            List.of("amount", "price", "ratio", "rate", "deposit", "fee", "discount", "percent");

    private final SysConfigMapper configMapper;
    private final CacheStore cacheStore;
    private final ObjectMapper objectMapper;

    public ConfigService(SysConfigMapper configMapper, CacheStore cacheStore, ObjectMapper objectMapper) {
        this.configMapper = configMapper;
        this.cacheStore = cacheStore;
        this.objectMapper = objectMapper;
    }

    /** 全部分组参数，按分组聚合。 */
    public List<ConfigGroupVO> groups() {
        List<SysConfig> rows = configMapper.selectList(Wrappers.<SysConfig>lambdaQuery()
                .orderByAsc(SysConfig::getConfigGroup)
                .orderByAsc(SysConfig::getConfigKey));
        Map<String, List<ConfigGroupVO.Item>> grouped = new LinkedHashMap<>();
        for (SysConfig row : rows) {
            grouped.computeIfAbsent(row.getConfigGroup(), key -> new ArrayList<>())
                    .add(new ConfigGroupVO.Item(row.getConfigKey(), row.getConfigValue(),
                            row.getValueType(), row.getRemark()));
        }
        List<ConfigGroupVO> result = new ArrayList<>(grouped.size());
        grouped.forEach((group, items) -> result.add(new ConfigGroupVO(group, items)));
        return result;
    }

    /**
     * 保存一个分组的参数。
     *
     * @return 实际变更的参数键
     */
    @Transactional
    public List<String> saveGroup(ConfigSaveRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        Map<String, SysConfig> existing = loadRows();
        List<String> sensitiveChanges = new ArrayList<>();
        for (ConfigSaveRequest.Item item : request.items()) {
            validateValue(item.value(), item.valueType());
            SysConfig old = existing.get(item.key());
            if (old != null && isSensitive(old) && !equalValue(old.getConfigValue(), item.value())) {
                sensitiveChanges.add(keyLabel(old));
            }
        }
        if (!sensitiveChanges.isEmpty() && !request.confirm()) {
            throw BusinessException.of(ErrorCode.CONFLICT,
                    "以下金额/比例参数发生变化，请确认后再次提交：" + String.join("、", sensitiveChanges));
        }

        List<String> changed = new ArrayList<>();
        for (ConfigSaveRequest.Item item : request.items()) {
            SysConfig old = existing.get(item.key());
            if (old == null) {
                SysConfig entity = new SysConfig();
                entity.setTenantId(tenantId);
                entity.setConfigGroup(request.group());
                entity.setConfigKey(item.key());
                entity.setConfigValue(item.value());
                entity.setValueType(item.valueType() == null ? "string" : item.valueType());
                entity.setRemark(item.remark());
                configMapper.insert(entity);
                changed.add(item.key());
                continue;
            }
            if (equalValue(old.getConfigValue(), item.value())) {
                continue;
            }
            old.setConfigValue(item.value());
            if (item.valueType() != null) {
                old.setValueType(item.valueType());
            }
            if (item.remark() != null) {
                old.setRemark(item.remark());
            }
            configMapper.updateById(old);
            changed.add(item.key());
        }
        evict(tenantId);
        log.info("系统参数保存完成 tenantId={} group={} changed={}", tenantId, request.group(), changed);
        return changed;
    }

    public String getString(String key, String defaultValue) {
        String value = cachedValues().get(key);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    public int getInt(String key, int defaultValue) {
        try {
            String value = getString(key, null);
            return value == null ? defaultValue : Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            log.warn("参数 {} 不是合法整数，返回默认值 {}", key, defaultValue);
            return defaultValue;
        }
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        String value = getString(key, null);
        return value == null ? defaultValue : Boolean.parseBoolean(value.trim());
    }

    public BigDecimal getDecimal(String key, BigDecimal defaultValue) {
        try {
            String value = getString(key, null);
            return value == null ? defaultValue : new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            log.warn("参数 {} 不是合法数值，返回默认值 {}", key, defaultValue);
            return defaultValue;
        }
    }

    private Map<String, SysConfig> loadRows() {
        List<SysConfig> rows = configMapper.selectList(Wrappers.<SysConfig>lambdaQuery());
        Map<String, SysConfig> map = new LinkedHashMap<>();
        for (SysConfig row : rows) {
            map.put(row.getConfigKey(), row);
        }
        return map;
    }

    private Map<String, String> cachedValues() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            return Map.of();
        }
        String cacheKey = CACHE_PREFIX + tenantId;
        String cached = cacheStore.get(cacheKey);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, new TypeReference<Map<String, String>>() {
                });
            } catch (Exception e) {
                log.warn("系统参数缓存解析失败 tenantId={}", tenantId, e);
            }
        }
        Map<String, String> values = new LinkedHashMap<>();
        for (SysConfig row : loadRows().values()) {
            values.put(row.getConfigKey(), row.getConfigValue());
        }
        try {
            cacheStore.put(cacheKey, objectMapper.writeValueAsString(values), TTL);
        } catch (Exception e) {
            log.warn("系统参数缓存写入失败 tenantId={}", tenantId, e);
        }
        return values;
    }

    private void validateValue(String value, String valueType) {
        if (value == null || value.isBlank()) {
            return;
        }
        String type = valueType == null ? "string" : valueType;
        try {
            switch (type) {
                case "number" -> new BigDecimal(value.trim());
                case "boolean" -> {
                    if (!"true".equalsIgnoreCase(value.trim()) && !"false".equalsIgnoreCase(value.trim())) {
                        throw BusinessException.of(ErrorCode.BAD_REQUEST, "参数值必须为 true 或 false：" + value);
                    }
                }
                case "json" -> objectMapper.readTree(value);
                default -> {
                    // 字符串不做格式约束
                }
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw BusinessException.of(ErrorCode.BAD_REQUEST, "参数值格式不正确：" + value);
        }
    }

    private boolean isSensitive(SysConfig config) {
        String key = config.getConfigKey() == null ? "" : config.getConfigKey().toLowerCase();
        for (String hint : SENSITIVE_HINTS) {
            if (key.contains(hint)) {
                return true;
            }
        }
        return false;
    }

    private String keyLabel(SysConfig config) {
        return config.getRemark() == null || config.getRemark().isBlank()
                ? config.getConfigKey()
                : config.getRemark() + "(" + config.getConfigKey() + ")";
    }

    private boolean equalValue(String left, String right) {
        String a = left == null ? "" : left.trim();
        String b = right == null ? "" : right.trim();
        return a.equals(b);
    }

    public void evict(Long tenantId) {
        cacheStore.delete(CACHE_PREFIX + tenantId);
    }
}
