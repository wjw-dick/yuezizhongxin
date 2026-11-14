package com.yzz.content.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yzz.common.api.BusinessException;
import com.yzz.common.api.ErrorCode;
import com.yzz.common.sensitive.SensitiveWordFilter;
import com.yzz.common.tenant.TenantContext;
import com.yzz.content.entity.CmsContent;
import com.yzz.content.entity.CmsContentI18n;
import com.yzz.content.mapper.CmsContentI18nMapper;
import com.yzz.content.mapper.CmsContentMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 内容 CMS（T48）。
 *
 * <p>发布前三道闸：适用端必须选择、案例必须有客户授权、正文过敏感词。
 * 多语言内容按语言分行存储，读取时缺失语言回退到默认语言。</p>
 */
@Service
public class CmsService {

    private static final List<String> TYPES = List.of(
            "Banner", "品牌故事", "房型", "康复", "餐食", "团队", "案例", "常见问题", "资讯");

    private final CmsContentMapper contentMapper;
    private final CmsContentI18nMapper i18nMapper;
    private final SensitiveWordFilter sensitiveWordFilter;

    public CmsService(CmsContentMapper contentMapper, CmsContentI18nMapper i18nMapper,
                      SensitiveWordFilter sensitiveWordFilter) {
        this.contentMapper = contentMapper;
        this.i18nMapper = i18nMapper;
        this.sensitiveWordFilter = sensitiveWordFilter;
    }

    public List<CmsContent> list(String contentType, String status, String keyword) {
        return contentMapper.selectList(Wrappers.<CmsContent>lambdaQuery()
                .eq(contentType != null && !contentType.isBlank(), CmsContent::getContentType, contentType)
                .eq(status != null && !status.isBlank(), CmsContent::getStatus, status)
                .like(keyword != null && !keyword.isBlank(), CmsContent::getTitle, keyword)
                .orderByDesc(CmsContent::getId));
    }

    public Map<String, Object> detail(Long id) {
        CmsContent content = require(id);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", content);
        result.put("i18n", i18nMapper.selectList(Wrappers.<CmsContentI18n>lambdaQuery()
                .eq(CmsContentI18n::getContentId, id)));
        return result;
    }

    @Transactional
    public Long save(CmsContent input) {
        if (input.getContentType() == null || !TYPES.contains(input.getContentType())) {
            throw BusinessException.of(ErrorCode.BAD_REQUEST, "内容类型不合法");
        }
        if (input.getTitle() == null || input.getTitle().isBlank()) {
            throw BusinessException.of(ErrorCode.BAD_REQUEST, "标题不能为空");
        }
        sensitiveWordFilter.assertClean(input.getTitle() + " " + (input.getBodyHtml() == null ? "" : input.getBodyHtml()));
        input.setTenantId(TenantContext.requireTenantId());
        if (input.getStatus() == null) {
            input.setStatus("草稿");
        }
        if (input.getAuthorized() == null) {
            input.setAuthorized(0);
        }
        if (input.getId() == null) {
            contentMapper.insert(input);
        } else {
            contentMapper.updateById(input);
        }
        return input.getId();
    }

    /**
     * 发布：校验适用端、案例授权与敏感词，成功后记录发布时间。
     */
    @Transactional
    public void publish(Long id, String applyEnds) {
        CmsContent content = require(id);
        String ends = applyEnds == null || applyEnds.isBlank() ? content.getApplyEnds() : applyEnds;
        if (ends == null || ends.isBlank()) {
            throw BusinessException.of(ErrorCode.BAD_REQUEST, "发布前必须选择适用端");
        }
        if ("案例".equals(content.getContentType()) && !Integer.valueOf(1).equals(content.getAuthorized())) {
            throw BusinessException.of(ErrorCode.BAD_REQUEST, "案例发布前必须取得客户授权并勾选授权标记");
        }
        sensitiveWordFilter.assertClean(content.getTitle() + " " + (content.getBodyHtml() == null ? "" : content.getBodyHtml()));
        content.setApplyEnds(ends);
        content.setStatus("已发布");
        content.setPublishedTime(LocalDateTime.now());
        contentMapper.updateById(content);
    }

    @Transactional
    public void offline(Long id) {
        CmsContent content = require(id);
        content.setStatus("已下线");
        contentMapper.updateById(content);
    }

    @Transactional
    public void saveI18n(Long contentId, List<CmsContentI18n> rows) {
        require(contentId);
        i18nMapper.delete(Wrappers.<CmsContentI18n>lambdaQuery()
                .eq(CmsContentI18n::getContentId, contentId));
        for (CmsContentI18n row : rows) {
            row.setId(null);
            row.setTenantId(TenantContext.requireTenantId());
            row.setContentId(contentId);
            row.setTranslated(row.getTitle() != null && !row.getTitle().isBlank() ? 1 : 0);
            i18nMapper.insert(row);
        }
    }

    /* ---------------- 前台读取 ---------------- */

    public List<Map<String, Object>> publicList(String contentType, String lang, String end) {
        List<CmsContent> rows = contentMapper.selectList(Wrappers.<CmsContent>lambdaQuery()
                .eq(CmsContent::getStatus, "已发布")
                .eq(contentType != null && !contentType.isBlank(), CmsContent::getContentType, contentType)
                .like(end != null && !end.isBlank(), CmsContent::getApplyEnds, end)
                .orderByDesc(CmsContent::getPublishedTime)
                .last("limit 100"));
        return rows.stream().map(row -> translate(row, lang)).toList();
    }

    public Map<String, Object> publicDetail(Long id, String lang) {
        CmsContent content = require(id);
        if (!"已发布".equals(content.getStatus())) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "内容未发布");
        }
        return translate(content, lang);
    }

    /** 首页聚合：Banner、品牌故事、案例、资讯、常见问题各取若干。 */
    public Map<String, Object> home(String lang) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("banners", publicList("Banner", lang, "官网"));
        result.put("brand", publicList("品牌故事", lang, "官网").stream().findFirst().orElse(null));
        result.put("cases", limit(publicList("案例", lang, "官网"), 4));
        result.put("news", limit(publicList("资讯", lang, "官网"), 3));
        result.put("faq", limit(publicList("常见问题", lang, "官网"), 6));
        result.put("team", limit(publicList("团队", lang, "官网"), 4));
        return result;
    }

    public CmsContent require(Long id) {
        CmsContent content = contentMapper.selectById(id);
        if (content == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "内容不存在");
        }
        return content;
    }

    private Map<String, Object> translate(CmsContent content, String lang) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", content.getId());
        map.put("contentType", content.getContentType());
        map.put("title", content.getTitle());
        map.put("cover", content.getCover());
        map.put("bodyHtml", content.getBodyHtml());
        map.put("seoTitle", content.getSeoTitle());
        map.put("seoKeywords", content.getSeoKeywords());
        map.put("seoDescription", content.getSeoDescription());
        map.put("publishedTime", content.getPublishedTime());
        if (lang != null && !lang.isBlank()) {
            CmsContentI18n i18n = i18nMapper.selectOne(Wrappers.<CmsContentI18n>lambdaQuery()
                    .eq(CmsContentI18n::getContentId, content.getId())
                    .eq(CmsContentI18n::getLangCode, lang).last("limit 1"));
            if (i18n != null) {
                map.put("title", i18n.getTitle() == null ? content.getTitle() : i18n.getTitle());
                map.put("bodyHtml", i18n.getBodyHtml() == null ? content.getBodyHtml() : i18n.getBodyHtml());
                map.put("lang", lang);
            }
        }
        return map;
    }

    private List<Map<String, Object>> limit(List<Map<String, Object>> rows, int size) {
        return rows.size() <= size ? rows : new ArrayList<>(rows.subList(0, size));
    }
}
