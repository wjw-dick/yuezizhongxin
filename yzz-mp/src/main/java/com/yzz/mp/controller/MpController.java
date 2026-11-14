package com.yzz.mp.controller;

import com.yzz.common.api.PageResult;
import com.yzz.common.api.R;
import com.yzz.common.tenant.AuthGuard;
import com.yzz.mp.entity.MpAccount;
import com.yzz.mp.entity.MpAutoReply;
import com.yzz.mp.entity.MpDraft;
import com.yzz.mp.entity.MpFan;
import com.yzz.mp.entity.MpMaterial;
import com.yzz.mp.entity.MpMenuConfig;
import com.yzz.mp.entity.MpMessage;
import com.yzz.mp.entity.MpTag;
import com.yzz.mp.service.MpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 公众号账号、标签、粉丝、消息、模板、菜单、自动回复、素材、图文与统计（T62–T70）。
 */
@Tag(name = "租户-公众号运营")
@RestController
@RequestMapping("/api/tenant/mp")
public class MpController {

    private final MpService mpService;

    public MpController(MpService mpService) {
        this.mpService = mpService;
    }

    @Operation(summary = "公众号列表")
    @GetMapping("/accounts")
    public R<List<MpAccount>> accounts() {
        AuthGuard.requireTenant();
        return R.ok(mpService.accounts());
    }

    @Operation(summary = "保存公众号与凭证")
    @PostMapping("/accounts")
    public R<Long> saveAccount(@RequestBody MpAccount request) {
        AuthGuard.requireTenant();
        return R.ok(mpService.saveAccount(request));
    }

    @Operation(summary = "删除公众号")
    @DeleteMapping("/accounts/{id}")
    public R<Void> deleteAccount(@PathVariable Long id) {
        AuthGuard.requireTenant();
        mpService.deleteAccount(id);
        return R.ok();
    }

    @Operation(summary = "凭证是否完善")
    @GetMapping("/accounts/{id}/ready")
    public R<Boolean> ready(@PathVariable Long id) {
        AuthGuard.requireTenant();
        return R.ok(mpService.credentialReady(id));
    }

    @Operation(summary = "生成带参二维码")
    @GetMapping("/accounts/{id}/qrcode")
    public R<Map<String, Object>> qrcode(@PathVariable Long id,
                                         @RequestParam(required = false) String scene) {
        AuthGuard.requireTenant();
        return R.ok(mpService.qrcode(id, scene));
    }

    @Operation(summary = "清空接口配额")
    @PostMapping("/accounts/{id}/clear-quota")
    public R<Void> clearQuota(@PathVariable Long id) {
        AuthGuard.requireTenant();
        mpService.clearQuota(id);
        return R.ok();
    }

    @Operation(summary = "标签列表")
    @GetMapping("/accounts/{id}/tags")
    public R<List<MpTag>> tags(@PathVariable Long id) {
        AuthGuard.requireTenant();
        return R.ok(mpService.tags(id));
    }

    @Operation(summary = "新增标签（先落库后推送）")
    @PostMapping("/accounts/{id}/tags")
    public R<Long> createTag(@PathVariable Long id, @RequestBody Map<String, String> body) {
        AuthGuard.requireTenant();
        return R.ok(mpService.createTag(id, body.get("name")));
    }

    @Operation(summary = "同步标签")
    @PostMapping("/accounts/{id}/tags/sync")
    public R<Integer> syncTags(@PathVariable Long id) {
        AuthGuard.requireTenant();
        return R.ok(mpService.syncTags(id));
    }

    @Operation(summary = "删除标签")
    @DeleteMapping("/tags/{tagId}")
    public R<Void> deleteTag(@PathVariable Long tagId) {
        AuthGuard.requireTenant();
        mpService.deleteTag(tagId);
        return R.ok();
    }

    @Operation(summary = "粉丝分页")
    @GetMapping("/accounts/{id}/fans")
    public R<PageResult<MpFan>> fans(@PathVariable Long id,
                                     @RequestParam(required = false) String keyword,
                                     @RequestParam(required = false) String subscribeStatus,
                                     @RequestParam(defaultValue = "1") int pageNum,
                                     @RequestParam(defaultValue = "10") int pageSize) {
        AuthGuard.requireTenant();
        return R.ok(mpService.fans(id, keyword, subscribeStatus, pageNum, Math.min(pageSize, 100)));
    }

    @Operation(summary = "同步粉丝")
    @PostMapping("/accounts/{id}/fans/sync")
    public R<Map<String, Object>> syncFans(@PathVariable Long id,
                                           @RequestParam(defaultValue = "200") int limit) {
        AuthGuard.requireTenant();
        return R.ok(mpService.syncFans(id, Math.min(limit, 1000)));
    }

    @Operation(summary = "修改粉丝备注")
    @PutMapping("/fans/{fanId}/remark")
    public R<Void> remark(@PathVariable Long fanId, @RequestBody Map<String, String> body) {
        AuthGuard.requireTenant();
        mpService.updateFanRemark(fanId, body.get("remark"));
        return R.ok();
    }

    @Operation(summary = "给粉丝打标签")
    @PostMapping("/fans/{fanId}/tags/{tagId}")
    public R<Void> tagFan(@PathVariable Long fanId, @PathVariable Long tagId) {
        AuthGuard.requireTenant();
        mpService.tagFan(fanId, tagId);
        return R.ok();
    }

    @Operation(summary = "消息流水")
    @GetMapping("/accounts/{id}/messages")
    public R<PageResult<MpMessage>> messages(@PathVariable Long id,
                                             @RequestParam(required = false) String msgType,
                                             @RequestParam(required = false)
                                             @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                             @RequestParam(required = false)
                                             @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                             @RequestParam(defaultValue = "1") int pageNum,
                                             @RequestParam(defaultValue = "10") int pageSize) {
        AuthGuard.requireTenant();
        return R.ok(mpService.messages(id, msgType, from, to, pageNum, Math.min(pageSize, 100)));
    }

    @Operation(summary = "模板消息列表（只读）")
    @GetMapping("/accounts/{id}/templates")
    public R<List<Map<String, Object>>> templates(@PathVariable Long id) {
        AuthGuard.requireTenant();
        return R.ok(mpService.templates(id));
    }

    @Operation(summary = "菜单配置列表")
    @GetMapping("/accounts/{id}/menus")
    public R<List<MpMenuConfig>> menus(@PathVariable Long id) {
        AuthGuard.requireTenant();
        return R.ok(mpService.menus(id));
    }

    @Operation(summary = "保存菜单配置")
    @PostMapping("/accounts/{id}/menus")
    public R<Long> saveMenu(@PathVariable Long id, @RequestBody MpMenuConfig request) {
        AuthGuard.requireTenant();
        request.setAccountId(id);
        return R.ok(mpService.saveMenu(request));
    }

    @Operation(summary = "发布菜单")
    @PostMapping("/menus/{menuId}/publish")
    public R<Map<String, Object>> publishMenu(@PathVariable Long menuId) {
        AuthGuard.requireTenant();
        return R.ok(mpService.publishMenu(menuId));
    }

    @Operation(summary = "清空菜单")
    @PostMapping("/accounts/{id}/menus/clear")
    public R<Void> clearMenu(@PathVariable Long id) {
        AuthGuard.requireTenant();
        mpService.clearMenu(id);
        return R.ok();
    }

    @Operation(summary = "自动回复列表")
    @GetMapping("/accounts/{id}/replies")
    public R<List<MpAutoReply>> replies(@PathVariable Long id,
                                        @RequestParam(required = false) String replyType) {
        AuthGuard.requireTenant();
        return R.ok(mpService.replies(id, replyType));
    }

    @Operation(summary = "保存自动回复")
    @PostMapping("/accounts/{id}/replies")
    public R<Long> saveReply(@PathVariable Long id, @RequestBody MpAutoReply request) {
        AuthGuard.requireTenant();
        request.setAccountId(id);
        return R.ok(mpService.saveReply(request));
    }

    @Operation(summary = "删除自动回复")
    @DeleteMapping("/replies/{id}")
    public R<Void> deleteReply(@PathVariable Long id) {
        AuthGuard.requireTenant();
        mpService.deleteReply(id);
        return R.ok();
    }

    @Operation(summary = "素材列表")
    @GetMapping("/accounts/{id}/materials")
    public R<List<MpMaterial>> materials(@PathVariable Long id,
                                         @RequestParam(required = false) String type) {
        AuthGuard.requireTenant();
        return R.ok(mpService.materials(id, type));
    }

    @Operation(summary = "登记素材（大小校验）")
    @PostMapping("/accounts/{id}/materials")
    public R<MpMaterial> uploadMaterial(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        AuthGuard.requireTenant();
        return R.ok(mpService.uploadMaterial(id, String.valueOf(body.get("name")),
                String.valueOf(body.getOrDefault("type", "图片")),
                Long.parseLong(String.valueOf(body.getOrDefault("sizeBytes", "0"))),
                (String) body.get("fileKey"), (String) body.get("thumbMediaId")));
    }

    @Operation(summary = "图文草稿与发表记录")
    @GetMapping("/accounts/{id}/drafts")
    public R<List<MpDraft>> drafts(@PathVariable Long id,
                                   @RequestParam(required = false) String status) {
        AuthGuard.requireTenant();
        return R.ok(mpService.drafts(id, status));
    }

    @Operation(summary = "保存图文草稿")
    @PostMapping("/accounts/{id}/drafts")
    public R<Long> saveDraft(@PathVariable Long id, @RequestBody MpDraft request) {
        AuthGuard.requireTenant();
        request.setAccountId(id);
        return R.ok(mpService.saveDraft(request));
    }

    @Operation(summary = "发表图文")
    @PostMapping("/drafts/{draftId}/publish")
    public R<Map<String, Object>> publishDraft(@PathVariable Long draftId) {
        AuthGuard.requireTenant();
        return R.ok(mpService.publishDraft(draftId));
    }

    @Operation(summary = "数据统计（4 张固定图）")
    @GetMapping("/accounts/{id}/stats")
    public R<Map<String, Object>> stats(@PathVariable Long id,
                                        @RequestParam(required = false)
                                        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                        @RequestParam(required = false)
                                        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        AuthGuard.requireTenant();
        return R.ok(mpService.stats(id, from, to));
    }
}
