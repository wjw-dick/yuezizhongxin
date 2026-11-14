package com.yzz.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yzz.common.api.BusinessException;
import com.yzz.common.api.ErrorCode;
import com.yzz.common.guard.CacheStore;
import com.yzz.system.dto.I18nMessageRequest;
import com.yzz.system.dto.LanguageRequest;
import com.yzz.system.entity.SysI18nMessage;
import com.yzz.system.entity.SysLanguage;
import com.yzz.system.mapper.SysI18nMessageMapper;
import com.yzz.system.mapper.SysLanguageMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 多语言（T21）。
 *
 * <p>语言列表全平台共用，语言包按语言编码缓存；缺词时回落默认语言，
 * 默认语言禁止停用，切换默认语言自动清除其他默认标记。</p>
 */
@Service
public class I18nService {

    private static final Logger log = LoggerFactory.getLogger(I18nService.class);
    private static final String PACK_PREFIX = "i18n:pack:";
    private static final Duration TTL = Duration.ofMinutes(30);

    private final SysLanguageMapper languageMapper;
    private final SysI18nMessageMapper messageMapper;
    private final CacheStore cacheStore;
    private final ObjectMapper objectMapper;

    public I18nService(SysLanguageMapper languageMapper, SysI18nMessageMapper messageMapper,
                       CacheStore cacheStore, ObjectMapper objectMapper) {
        this.languageMapper = languageMapper;
        this.messageMapper = messageMapper;
        this.cacheStore = cacheStore;
        this.objectMapper = objectMapper;
    }

    public List<SysLanguage> languages() {
        return languageMapper.selectList(Wrappers.<SysLanguage>lambdaQuery()
                .orderByDesc(SysLanguage::getIsDefault)
                .orderByAsc(SysLanguage::getSort)
                .orderByAsc(SysLanguage::getId));
    }

    @Transactional
    public void saveLanguage(Long id, LanguageRequest request) {
        SysLanguage entity = id == null ? null : languageMapper.selectById(id);
        if (entity == null) {
            SysLanguage exists = languageMapper.selectOne(Wrappers.<SysLanguage>lambdaQuery()
                    .eq(SysLanguage::getCode, request.code()).last("limit 1"));
            if (exists != null) {
                throw BusinessException.of(ErrorCode.CONFLICT, "语言编码已存在：" + request.code());
            }
            entity = new SysLanguage();
            entity.setCode(request.code().trim());
            entity.setName(request.name().trim());
            entity.setEnabled(request.enabled() == null || request.enabled() ? 1 : 0);
            entity.setSort(request.sort() == null ? 0 : request.sort());
            entity.setIsDefault(0);
            languageMapper.insert(entity);
        } else {
            if (Boolean.FALSE.equals(request.enabled()) && entity.getIsDefault() != null
                    && entity.getIsDefault() == 1) {
                throw BusinessException.of(ErrorCode.CONFLICT, "默认语言不能停用，请先切换默认语言");
            }
            entity.setName(request.name().trim());
            if (request.enabled() != null) {
                entity.setEnabled(request.enabled() ? 1 : 0);
            }
            if (request.sort() != null) {
                entity.setSort(request.sort());
            }
            languageMapper.updateById(entity);
        }
        if (Boolean.TRUE.equals(request.isDefault())) {
            setDefault(entity.getId());
        }
        evict(entity.getCode());
    }

    @Transactional
    public void setDefault(Long id) {
        SysLanguage target = languageMapper.selectById(id);
        if (target == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "语言不存在");
        }
        if (target.getEnabled() != null && target.getEnabled() == 0) {
            throw BusinessException.of(ErrorCode.CONFLICT, "停用的语言不能设为默认");
        }
        for (SysLanguage language : languages()) {
            int flag = language.getId().equals(id) ? 1 : 0;
            if (language.getIsDefault() != null && language.getIsDefault() == flag) {
                continue;
            }
            SysLanguage update = new SysLanguage();
            update.setId(language.getId());
            update.setIsDefault(flag);
            languageMapper.updateById(update);
            evict(language.getCode());
        }
    }

    @Transactional
    public void deleteLanguage(Long id) {
        SysLanguage entity = languageMapper.selectById(id);
        if (entity == null) {
            return;
        }
        if (entity.getIsDefault() != null && entity.getIsDefault() == 1) {
            throw BusinessException.of(ErrorCode.CONFLICT, "默认语言不能删除");
        }
        languageMapper.deleteById(id);
        messageMapper.delete(Wrappers.<SysI18nMessage>lambdaQuery()
                .eq(SysI18nMessage::getLangCode, entity.getCode()));
        evict(entity.getCode());
    }

    public Map<String, String> pack(String langCode) {
        String cacheKey = PACK_PREFIX + langCode;
        String cached = cacheStore.get(cacheKey);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, new TypeReference<Map<String, String>>() {
                });
            } catch (Exception e) {
                log.warn("语言包缓存解析失败 lang={}", langCode, e);
            }
        }
        Map<String, String> merged = new LinkedHashMap<>();
        String defaultCode = defaultCode();
        if (defaultCode != null && !defaultCode.equals(langCode)) {
            merged.putAll(messagesOf(defaultCode));
        }
        merged.putAll(messagesOf(langCode));
        try {
            cacheStore.put(cacheKey, objectMapper.writeValueAsString(merged), TTL);
        } catch (Exception e) {
            log.warn("语言包缓存写入失败 lang={}", langCode, e);
        }
        return merged;
    }

    @Transactional
    public int saveMessages(I18nMessageRequest request) {
        SysLanguage language = languageMapper.selectOne(Wrappers.<SysLanguage>lambdaQuery()
                .eq(SysLanguage::getCode, request.langCode()).last("limit 1"));
        if (language == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "语言不存在：" + request.langCode());
        }
        int count = 0;
        for (Map.Entry<String, String> entry : request.messages().entrySet()) {
            String key = entry.getKey() == null ? "" : entry.getKey().trim();
            if (key.isEmpty()) {
                continue;
            }
            SysI18nMessage exists = messageMapper.selectOne(Wrappers.<SysI18nMessage>lambdaQuery()
                    .eq(SysI18nMessage::getLangCode, request.langCode())
                    .eq(SysI18nMessage::getMsgKey, key).last("limit 1"));
            if (exists == null) {
                SysI18nMessage entity = new SysI18nMessage();
                entity.setLangCode(request.langCode());
                entity.setMsgKey(key);
                entity.setMsgValue(entry.getValue());
                messageMapper.insert(entity);
            } else {
                exists.setMsgValue(entry.getValue());
                messageMapper.updateById(exists);
            }
            count++;
        }
        cacheStore.delete(PACK_PREFIX + request.langCode());
        if (defaultCode() != null) {
            cacheStore.delete(PACK_PREFIX + defaultCode());
        }
        return count;
    }

    public List<SysI18nMessage> messages(String langCode) {
        return messageMapper.selectList(Wrappers.<SysI18nMessage>lambdaQuery()
                .eq(SysI18nMessage::getLangCode, langCode)
                .orderByAsc(SysI18nMessage::getMsgKey));
    }

    public String defaultCode() {
        SysLanguage entity = languageMapper.selectOne(Wrappers.<SysLanguage>lambdaQuery()
                .eq(SysLanguage::getIsDefault, 1).last("limit 1"));
        if (entity != null) {
            return entity.getCode();
        }
        List<SysLanguage> all = languages();
        return all.isEmpty() ? null : all.get(0).getCode();
    }

    private Map<String, String> messagesOf(String langCode) {
        Map<String, String> map = new LinkedHashMap<>();
        if (langCode == null) {
            return map;
        }
        for (SysI18nMessage message : messages(langCode)) {
            map.put(message.getMsgKey(), message.getMsgValue());
        }
        return map;
    }

    private void evict(String langCode) {
        cacheStore.delete(PACK_PREFIX + langCode);
        List<String> all = new ArrayList<>();
        for (SysLanguage language : languages()) {
            all.add(language.getCode());
        }
        for (String code : all) {
            cacheStore.delete(PACK_PREFIX + code);
        }
    }
}
