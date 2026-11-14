package com.yzz.system.controller;

import com.yzz.common.api.R;
import com.yzz.system.service.DictService;
import com.yzz.system.service.I18nService;
import com.yzz.system.service.SsoService;
import com.yzz.system.vo.DictOptionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 前台开放接口：官网、小程序、H5 读取字典、语言包与登录方式，不需要登录态。
 *
 * <p>租户仍由请求头或子域名解析，返回值只包含当前租户可见的字典与文案。</p>
 */
@Tag(name = "开放接口-基础数据")
@RestController
@RequestMapping("/api/open")
public class OpenDataController {

    private final DictService dictService;
    private final I18nService i18nService;
    private final SsoService ssoService;

    public OpenDataController(DictService dictService, I18nService i18nService,
                              SsoService ssoService) {
        this.dictService = dictService;
        this.i18nService = i18nService;
        this.ssoService = ssoService;
    }

    @Operation(summary = "字典选项")
    @GetMapping("/dicts/{category}")
    public R<List<DictOptionVO>> dict(@PathVariable String category) {
        return R.ok(dictService.options(category));
    }

    @Operation(summary = "语言包")
    @GetMapping("/i18n/{langCode}")
    public R<Map<String, String>> pack(@PathVariable String langCode) {
        return R.ok(i18nService.pack(langCode));
    }

    @Operation(summary = "语言列表与默认语言")
    @GetMapping("/i18n/languages")
    public R<Map<String, Object>> languages() {
        return R.ok(Map.of(
                "default", i18nService.defaultCode() == null ? "zh-CN" : i18nService.defaultCode(),
                "items", i18nService.languages()));
    }

    @Operation(summary = "登录方式（是否启用 SSO）")
    @GetMapping("/login-options")
    public R<SsoService.SsoLoginInfo> loginOptions() {
        return R.ok(ssoService.loginInfo());
    }
}
