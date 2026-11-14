package com.yzz.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yzz.common.api.BusinessException;
import com.yzz.common.api.ErrorCode;
import com.yzz.common.guard.CacheStore;
import com.yzz.system.dto.DictCategoryRequest;
import com.yzz.system.dto.DictItemRequest;
import com.yzz.system.entity.SysDictCategory;
import com.yzz.system.entity.SysDictItem;
import com.yzz.system.mapper.SysDictCategoryMapper;
import com.yzz.system.mapper.SysDictItemMapper;
import com.yzz.system.vo.DictOptionVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据字典（T29）。
 *
 * <p>字典由平台统一下发，中文名称是页面唯一渲染值，编码仅供接口传参与前端取值。
 * 下拉选项走缓存，写操作立即失效缓存，保证「保存即时生效」。</p>
 */
@Service
public class DictService {

    private static final Logger log = LoggerFactory.getLogger(DictService.class);
    private static final String CACHE_PREFIX = "dict:options:";
    private static final Duration TTL = Duration.ofMinutes(10);

    private final SysDictCategoryMapper categoryMapper;
    private final SysDictItemMapper itemMapper;
    private final CacheStore cacheStore;
    private final ObjectMapper objectMapper;

    public DictService(SysDictCategoryMapper categoryMapper, SysDictItemMapper itemMapper,
                       CacheStore cacheStore, ObjectMapper objectMapper) {
        this.categoryMapper = categoryMapper;
        this.itemMapper = itemMapper;
        this.cacheStore = cacheStore;
        this.objectMapper = objectMapper;
    }

    public List<SysDictCategory> categories() {
        return categoryMapper.selectList(Wrappers.<SysDictCategory>lambdaQuery()
                .orderByAsc(SysDictCategory::getSort)
                .orderByAsc(SysDictCategory::getId));
    }

    public void createCategory(DictCategoryRequest request) {
        SysDictCategory exists = categoryMapper.selectOne(Wrappers.<SysDictCategory>lambdaQuery()
                .eq(SysDictCategory::getCategory, request.category()).last("limit 1"));
        if (exists != null) {
            throw BusinessException.of(ErrorCode.CONFLICT, "字典分类编码已存在：" + request.category());
        }
        SysDictCategory entity = new SysDictCategory();
        entity.setCategory(request.category().trim());
        entity.setName(request.name().trim());
        entity.setSort(request.sort() == null ? 0 : request.sort());
        entity.setStatus(request.status() == null ? "ENABLED" : request.status());
        categoryMapper.insert(entity);
    }

    public void updateCategory(Long id, DictCategoryRequest request) {
        SysDictCategory entity = requireCategory(id);
        entity.setName(request.name().trim());
        entity.setSort(request.sort() == null ? entity.getSort() : request.sort());
        entity.setStatus(request.status() == null ? entity.getStatus() : request.status());
        categoryMapper.updateById(entity);
        evict(entity.getCategory());
    }

    /** 分类下仍有字典项时禁止删除，避免页面出现空字典。 */
    @Transactional
    public void deleteCategory(Long id) {
        SysDictCategory entity = requireCategory(id);
        Long count = itemMapper.selectCount(Wrappers.<SysDictItem>lambdaQuery()
                .eq(SysDictItem::getCategory, entity.getCategory()));
        if (count != null && count > 0) {
            throw BusinessException.of(ErrorCode.CONFLICT, "该分类下仍有 " + count + " 个字典项，请先清理");
        }
        categoryMapper.deleteById(id);
        evict(entity.getCategory());
    }

    public List<SysDictItem> items(String category) {
        return itemMapper.selectList(Wrappers.<SysDictItem>lambdaQuery()
                .eq(SysDictItem::getCategory, category)
                .orderByAsc(SysDictItem::getSort)
                .orderByAsc(SysDictItem::getId));
    }

    public void createItem(String category, DictItemRequest request) {
        requireCategoryByCode(category);
        SysDictItem exists = itemMapper.selectOne(Wrappers.<SysDictItem>lambdaQuery()
                .eq(SysDictItem::getCategory, category)
                .eq(SysDictItem::getItemCode, request.code()).last("limit 1"));
        if (exists != null) {
            throw BusinessException.of(ErrorCode.CONFLICT, "该分类下字典项编码已存在：" + request.code());
        }
        SysDictItem entity = new SysDictItem();
        entity.setCategory(category);
        entity.setItemCode(request.code().trim());
        entity.setItemName(request.name().trim());
        entity.setColor(request.color());
        entity.setSort(request.sort() == null ? 0 : request.sort());
        entity.setStatus(request.status() == null ? "ENABLED" : request.status());
        itemMapper.insert(entity);
        evict(category);
    }

    public void updateItem(Long id, DictItemRequest request) {
        SysDictItem entity = requireItem(id);
        entity.setItemName(request.name().trim());
        entity.setColor(request.color());
        entity.setSort(request.sort() == null ? entity.getSort() : request.sort());
        entity.setStatus(request.status() == null ? entity.getStatus() : request.status());
        itemMapper.updateById(entity);
        evict(entity.getCategory());
    }

    public void deleteItem(Long id) {
        SysDictItem entity = requireItem(id);
        itemMapper.deleteById(id);
        evict(entity.getCategory());
    }

    /**
     * 下拉选项：只返回启用项，value 为编码，label 为中文名。
     * 前端禁止直接渲染 value（编码仅内部使用）。
     */
    public List<DictOptionVO> options(String category) {
        String cacheKey = CACHE_PREFIX + category;
        String cached = cacheStore.get(cacheKey);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, new TypeReference<List<DictOptionVO>>() {
                });
            } catch (Exception e) {
                log.warn("字典缓存解析失败，回源查询 category={}", category, e);
            }
        }
        List<SysDictItem> items = itemMapper.selectList(Wrappers.<SysDictItem>lambdaQuery()
                .eq(SysDictItem::getCategory, category)
                .eq(SysDictItem::getStatus, "ENABLED")
                .orderByAsc(SysDictItem::getSort)
                .orderByAsc(SysDictItem::getId));
        List<DictOptionVO> options = new ArrayList<>(items.size());
        for (SysDictItem item : items) {
            options.add(new DictOptionVO(item.getItemCode(), item.getItemName(), item.getColor()));
        }
        try {
            cacheStore.put(cacheKey, objectMapper.writeValueAsString(options), TTL);
        } catch (Exception e) {
            log.warn("字典缓存写入失败 category={}", category, e);
        }
        return options;
    }

    /** 取字典中文名，供报表、消息模板等按编码反查展示值。 */
    public String nameOf(String category, String code) {
        for (DictOptionVO option : options(category)) {
            if (option.value().equals(code)) {
                return option.label();
            }
        }
        return null;
    }

    private SysDictCategory requireCategory(Long id) {
        SysDictCategory entity = categoryMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "字典分类不存在");
        }
        return entity;
    }

    private SysDictCategory requireCategoryByCode(String category) {
        SysDictCategory entity = categoryMapper.selectOne(Wrappers.<SysDictCategory>lambdaQuery()
                .eq(SysDictCategory::getCategory, category).last("limit 1"));
        if (entity == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "字典分类不存在：" + category);
        }
        return entity;
    }

    private SysDictItem requireItem(Long id) {
        SysDictItem entity = itemMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "字典项不存在");
        }
        return entity;
    }

    private void evict(String category) {
        cacheStore.delete(CACHE_PREFIX + category);
    }
}
