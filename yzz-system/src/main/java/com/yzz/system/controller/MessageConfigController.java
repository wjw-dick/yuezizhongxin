package com.yzz.system.controller;

import com.yzz.common.api.R;
import com.yzz.common.audit.AuditLog;
import com.yzz.common.tenant.AuthGuard;
import com.yzz.system.dto.MsgChannelRequest;
import com.yzz.system.dto.MsgTemplateRequest;
import com.yzz.system.entity.MsgTemplate;
import com.yzz.system.service.MessageConfigService;
import com.yzz.system.vo.MsgChannelVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 消息配置（T27）：通道卡片、渠道单选启用、测试发送、模板与字段映射。
 */
@Tag(name = "租户-消息配置")
@RestController
@RequestMapping("/api/tenant/messages")
public class MessageConfigController {

    private final MessageConfigService messageService;

    public MessageConfigController(MessageConfigService messageService) {
        this.messageService = messageService;
    }

    @Operation(summary = "通道总览（4 消息通道 + 6 短信渠道）")
    @GetMapping("/channels")
    public R<Map<String, List<MsgChannelVO>>> channels() {
        AuthGuard.requireTenant();
        return R.ok(Map.of(
                "message", messageService.messageChannels(),
                "sms", messageService.smsChannels()));
    }

    @Operation(summary = "启用/停用短信渠道（单选）")
    @AuditLog(module = "消息配置", action = "切换短信渠道", withArgs = false)
    @PutMapping("/channels")
    public R<Void> saveChannel(@Valid @RequestBody MsgChannelRequest request) {
        AuthGuard.requireTenant();
        messageService.saveChannel(request);
        return R.ok();
    }

    @Operation(summary = "测试发送")
    @AuditLog(module = "消息配置", action = "测试发送")
    @PostMapping("/test")
    public R<String> test(@RequestParam String channelCode,
                          @RequestParam String phone,
                          @RequestParam(required = false) String content) {
        AuthGuard.requireTenant();
        return R.ok(messageService.testSend(channelCode, phone, content));
    }

    @Operation(summary = "模板列表")
    @GetMapping("/templates")
    public R<List<MsgTemplate>> templates(@RequestParam(required = false) String triggerNode,
                                          @RequestParam(required = false) String channel) {
        AuthGuard.requireTenant();
        return R.ok(messageService.templates(triggerNode, channel));
    }

    @Operation(summary = "保存模板（同节点同通道唯一）")
    @AuditLog(module = "消息配置", action = "保存模板")
    @PutMapping("/templates")
    public R<Void> saveTemplate(@Valid @RequestBody MsgTemplateRequest request) {
        AuthGuard.requireTenant();
        messageService.saveTemplate(request);
        return R.ok();
    }

    @Operation(summary = "删除模板")
    @AuditLog(module = "消息配置", action = "删除模板")
    @DeleteMapping("/templates/{id}")
    public R<Void> deleteTemplate(@PathVariable Long id) {
        AuthGuard.requireTenant();
        messageService.deleteTemplate(id);
        return R.ok();
    }
}
