package com.yzz.api.web;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yzz.common.api.R;
import com.yzz.common.api.BusinessException;
import com.yzz.common.api.ErrorCode;
import com.yzz.content.service.CmsService;
import com.yzz.product.entity.PkgPackage;
import com.yzz.product.service.PackageService;
import com.yzz.room.entity.RoomType;
import com.yzz.room.service.RoomService;
import com.yzz.system.entity.SysEntry;
import com.yzz.system.entity.SysNavNode;
import com.yzz.system.mapper.SysEntryMapper;
import com.yzz.system.mapper.SysNavNodeMapper;
import com.yzz.system.service.ConfigService;
import com.yzz.system.service.I18nService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 官网、H5、小程序读取站点数据（T56–T61、T72、T85–T87）。
 *
 * <p>聚合首页所需内容、在售套餐、房型与多端导航；只返回已发布内容与上架商品。
 * 租户由请求头 X-Tenant-Code 或子域名解析，缺租户上下文时返回 40102。</p>
 */
@Tag(name = "开放接口-站点数据")
@RestController
@RequestMapping("/api/open/site")
public class OpenSiteController {

    private final CmsService cmsService;
    private final PackageService packageService;
    private final RoomService roomService;
    private final ConfigService configService;
    private final I18nService i18nService;
    private final SysNavNodeMapper navMapper;
    private final SysEntryMapper entryMapper;

    public OpenSiteController(CmsService cmsService, PackageService packageService,
                              RoomService roomService, ConfigService configService,
                              I18nService i18nService, SysNavNodeMapper navMapper,
                              SysEntryMapper entryMapper) {
        this.cmsService = cmsService;
        this.packageService = packageService;
        this.roomService = roomService;
        this.configService = configService;
        this.i18nService = i18nService;
        this.navMapper = navMapper;
        this.entryMapper = entryMapper;
    }

    @Operation(summary = "首页聚合")
    @GetMapping("/home")
    public R<Map<String, Object>> home(@RequestParam(required = false) String lang) {
        requireTenant();
        Map<String, Object> result = new LinkedHashMap<>();
        result.putAll(cmsService.home(lang));
        result.put("packages", packageService.list("ON_SHELF", null));
        result.put("roomTypes", roomService.roomTypes().stream()
                .filter(t -> "ENABLED".equals(t.getStatus())).toList());
        result.put("site", siteInfo());
        result.put("nav", nav("PC官网"));
        return R.ok(result);
    }

    @Operation(summary = "内容列表")
    @GetMapping("/contents")
    public R<List<Map<String, Object>>> contents(@RequestParam(required = false) String type,
                                                 @RequestParam(required = false) String lang,
                                                 @RequestParam(required = false) String end) {
        requireTenant();
        return R.ok(cmsService.publicList(type, lang, end == null ? "官网" : end));
    }

    @Operation(summary = "内容详情")
    @GetMapping("/contents/{id}")
    public R<Map<String, Object>> content(@PathVariable Long id,
                                          @RequestParam(required = false) String lang) {
        requireTenant();
        return R.ok(cmsService.publicDetail(id, lang));
    }

    @Operation(summary = "房型列表")
    @GetMapping("/rooms")
    public R<List<RoomType>> rooms() {
        requireTenant();
        return R.ok(roomService.roomTypes().stream()
                .filter(t -> "ENABLED".equals(t.getStatus())).toList());
    }

    @Operation(summary = "套餐列表")
    @GetMapping("/packages")
    public R<List<PkgPackage>> packages() {
        requireTenant();
        return R.ok(packageService.list("ON_SHELF", null));
    }

    @Operation(summary = "多端导航")
    @GetMapping("/nav")
    public R<List<SysNavNode>> navs(@RequestParam(defaultValue = "PC官网") String end) {
        requireTenant();
        return R.ok(nav(end));
    }

    @Operation(summary = "站点基础信息与语言")
    @GetMapping("/meta")
    public R<Map<String, Object>> meta() {
        requireTenant();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("site", siteInfo());
        result.put("languages", i18nService.languages());
        result.put("defaultLang", i18nService.defaultCode());
        return R.ok(result);
    }

    @Operation(summary = "下载与入口（小程序码、App 下载）")
    @GetMapping("/entries")
    public R<List<SysEntry>> entries(@RequestParam(required = false) String type) {
        requireTenant();
        return R.ok(entryMapper.selectList(Wrappers.<SysEntry>lambdaQuery()
                .eq(type != null && !type.isBlank(), SysEntry::getEntryType, type)));
    }

    @Operation(summary = "入口扫码计数（来源归因）")
    @PostMapping("/entries/{id}/scan")
    public R<Void> scan(@PathVariable Long id) {
        SysEntry entry = entryMapper.selectById(id);
        if (entry == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "入口不存在");
        }
        entry.setScanCount(entry.getScanCount() == null ? 1 : entry.getScanCount() + 1);
        entryMapper.updateById(entry);
        return R.ok();
    }

    private Map<String, Object> siteInfo() {
        Map<String, Object> site = new LinkedHashMap<>();
        site.put("name", configService.getString("site.name", "月子中心"));
        site.put("phone", configService.getString("site.phone", "400-000-0000"));
        site.put("address", configService.getString("site.address", ""));
        site.put("icp", configService.getString("site.icp", ""));
        site.put("wechat", configService.getString("site.wechat", ""));
        return site;
    }

    private List<SysNavNode> nav(String end) {
        return navMapper.selectList(Wrappers.<SysNavNode>lambdaQuery()
                .eq(SysNavNode::getEndType, end)
                .eq(SysNavNode::getVisible, 1)
                .orderByAsc(SysNavNode::getParentId).orderByAsc(SysNavNode::getSort));
    }

    private void requireTenant() {
        if (com.yzz.common.tenant.TenantContext.getTenantId() == null) {
            throw BusinessException.of(ErrorCode.TENANT_NOT_FOUND);
        }
    }
}
