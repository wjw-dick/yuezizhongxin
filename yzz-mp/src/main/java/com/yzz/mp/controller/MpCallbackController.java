package com.yzz.mp.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yzz.common.util.SecretCipher;
import com.yzz.mp.client.WeChatMpClient;
import com.yzz.mp.entity.MpAccount;
import com.yzz.mp.mapper.MpAccountMapper;
import com.yzz.mp.service.MpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/**
 * 公众号服务器回调（T67）。
 *
 * <p>GET 用于微信服务器地址校验（原样回 echostr）；POST 接收消息与事件，
 * 落库后按关键词自动回复。验签失败一律拒绝，避免伪造消息写入。</p>
 */
@Tag(name = "开放接口-公众号回调")
@RestController
@RequestMapping("/api/open/mp")
public class MpCallbackController {

    private static final Logger log = LoggerFactory.getLogger(MpCallbackController.class);

    private final MpService mpService;
    private final MpAccountMapper accountMapper;
    private final SecretCipher secretCipher;

    public MpCallbackController(MpService mpService, MpAccountMapper accountMapper,
                                SecretCipher secretCipher) {
        this.mpService = mpService;
        this.accountMapper = accountMapper;
        this.secretCipher = secretCipher;
    }

    @Operation(summary = "服务器地址校验")
    @GetMapping(value = "/callback/{appId}", produces = MediaType.TEXT_PLAIN_VALUE)
    public String verify(@PathVariable String appId,
                         @RequestParam(required = false) String signature,
                         @RequestParam(required = false) String timestamp,
                         @RequestParam(required = false) String nonce,
                         @RequestParam(required = false) String echostr) {
        MpAccount account = findAccount(appId);
        if (account == null) {
            return "";
        }
        boolean ok = WeChatMpClient.verifySignature(tokenOf(account), timestamp, nonce, signature);
        log.info("公众号服务器校验 appId={} ok={}", appId, ok);
        return ok ? echostr : "";
    }

    @Operation(summary = "接收消息与事件")
    @PostMapping(value = "/callback/{appId}", produces = MediaType.APPLICATION_XML_VALUE)
    public String receive(@PathVariable String appId, @RequestBody String body,
                          HttpServletRequest request) {
        MpAccount account = findAccount(appId);
        if (account == null) {
            return "success";
        }
        if (!WeChatMpClient.verifySignature(tokenOf(account),
                request.getParameter("timestamp"), request.getParameter("nonce"),
                request.getParameter("signature"))) {
            log.warn("公众号消息验签失败 appId={}", appId);
            return "invalid signature";
        }
        String openId = text(body, "FromUserName");
        String msgType = text(body, "MsgType");
        String event = text(body, "Event");
        String content = text(body, "Content");
        mpService.receive(appId, openId, mapType(msgType, event), content, body, event);

        if ("text".equalsIgnoreCase(msgType)) {
            String reply = mpService.autoReply(account.getId(), content);
            if (reply != null && !reply.isBlank()) {
                return textReply(openId, account.getWechatId(), reply);
            }
        }
        if ("event".equalsIgnoreCase(msgType) && "subscribe".equalsIgnoreCase(event)) {
            String welcome = mpService.autoReply(account.getId(), "__subscribe__");
            if (welcome != null && !welcome.isBlank()) {
                return textReply(openId, account.getWechatId(), welcome);
            }
        }
        return "success";
    }

    private MpAccount findAccount(String appId) {
        return accountMapper.selectOne(Wrappers.<MpAccount>lambdaQuery()
                .eq(MpAccount::getAppId, appId).last("limit 1"));
    }

    /** 库内 token 为密文，验签前解密；未配置时返回空串，校验必然失败。 */
    private String tokenOf(MpAccount account) {
        if (account.getToken() == null || account.getToken().isBlank()) {
            return "";
        }
        try {
            return secretCipher.decrypt(account.getToken());
        } catch (Exception e) {
            log.warn("公众号 token 解密失败 appId={}", account.getAppId());
            return "";
        }
    }

    private String mapType(String msgType, String event) {
        if (msgType == null) {
            return "事件";
        }
        return switch (msgType) {
            case "text" -> "文本";
            case "image" -> "图片";
            case "voice" -> "语音";
            case "video" -> "视频";
            case "news" -> "图文";
            default -> event == null ? "事件" : "事件";
        };
    }

    private String text(String xml, String tag) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            Document doc = factory.newDocumentBuilder()
                    .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
            NodeList nodes = doc.getElementsByTagName(tag);
            return nodes.getLength() == 0 ? null : nodes.item(0).getTextContent();
        } catch (Exception e) {
            return null;
        }
    }

    private String textReply(String openId, String from, String content) {
        String safe = content.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        return "<xml><ToUserName><![CDATA[" + openId + "]]></ToUserName>"
                + "<FromUserName><![CDATA[" + (from == null ? "" : from) + "]]></FromUserName>"
                + "<CreateTime>" + System.currentTimeMillis() / 1000 + "</CreateTime>"
                + "<MsgType><![CDATA[text]]></MsgType>"
                + "<Content><![CDATA[" + safe + "]]></Content></xml>";
    }
}
