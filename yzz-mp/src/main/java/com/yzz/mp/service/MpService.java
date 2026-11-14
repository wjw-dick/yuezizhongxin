package com.yzz.mp.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yzz.common.api.BusinessException;
import com.yzz.common.api.ErrorCode;
import com.yzz.common.api.PageResult;
import com.yzz.common.guard.CacheStore;
import com.yzz.common.tenant.TenantContext;
import com.yzz.common.util.DesensitizeUtil;
import com.yzz.common.util.SecretCipher;
import com.yzz.mp.client.WeChatMpClient;
import com.yzz.mp.entity.MpAccount;
import com.yzz.mp.entity.MpAutoReply;
import com.yzz.mp.entity.MpDraft;
import com.yzz.mp.entity.MpFan;
import com.yzz.mp.entity.MpMaterial;
import com.yzz.mp.entity.MpMenuConfig;
import com.yzz.mp.entity.MpMessage;
import com.yzz.mp.entity.MpTag;
import com.yzz.mp.mapper.MpAccountMapper;
import com.yzz.mp.mapper.MpAutoReplyMapper;
import com.yzz.mp.mapper.MpDraftMapper;
import com.yzz.mp.mapper.MpFanMapper;
import com.yzz.mp.mapper.MpMaterialMapper;
import com.yzz.mp.mapper.MpMenuConfigMapper;
import com.yzz.mp.mapper.MpMessageMapper;
import com.yzz.mp.mapper.MpTagMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 公众号运营（T62–T70）。
 *
 * <p>凭证一律加密落库、接口只回掩码；未配置 AppID/AppSecret 时进入「本地模式」，
 * 统计与同步使用本地已落库数据，不向微信发起请求，页面明确标注「未接号」。
 * 所有微信错误码与错误描述原样落库到 mp_message，便于对照官方文档排查。</p>
 */
@Service
public class MpService {

    private static final Logger log = LoggerFactory.getLogger(MpService.class);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final MpAccountMapper accountMapper;
    private final MpTagMapper tagMapper;
    private final MpFanMapper fanMapper;
    private final MpMessageMapper messageMapper;
    private final MpMaterialMapper materialMapper;
    private final MpDraftMapper draftMapper;
    private final MpMenuConfigMapper menuMapper;
    private final MpAutoReplyMapper replyMapper;
    private final SecretCipher secretCipher;
    private final WeChatMpClient client;
    private final CacheStore cacheStore;
    private final ObjectMapper objectMapper;

    public MpService(MpAccountMapper accountMapper, MpTagMapper tagMapper, MpFanMapper fanMapper,
                     MpMessageMapper messageMapper, MpMaterialMapper materialMapper,
                     MpDraftMapper draftMapper, MpMenuConfigMapper menuMapper,
                     MpAutoReplyMapper replyMapper, SecretCipher secretCipher,
                     WeChatMpClient client, CacheStore cacheStore, ObjectMapper objectMapper) {
        this.accountMapper = accountMapper;
        this.tagMapper = tagMapper;
        this.fanMapper = fanMapper;
        this.messageMapper = messageMapper;
        this.materialMapper = materialMapper;
        this.draftMapper = draftMapper;
        this.menuMapper = menuMapper;
        this.replyMapper = replyMapper;
        this.secretCipher = secretCipher;
        this.client = client;
        this.cacheStore = cacheStore;
        this.objectMapper = objectMapper;
    }

    /* ---------------- 公众号台账与凭证（T62） ---------------- */

    public List<MpAccount> accounts() {
        List<MpAccount> rows = accountMapper.selectList(Wrappers.<MpAccount>lambdaQuery()
                .orderByDesc(MpAccount::getId));
        rows.forEach(this::maskAccount);
        return rows;
    }

    public MpAccount account(Long id) {
        MpAccount account = accountMapper.selectById(id);
        if (account == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "公众号不存在");
        }
        maskAccount(account);
        return account;
    }

    /** 凭证是否完善：其余子页在未完善时禁用。 */
    public boolean credentialReady(Long accountId) {
        MpAccount account = accountMapper.selectById(accountId);
        return client.configured(account) && account.getToken() != null && !account.getToken().isBlank();
    }

    @Transactional
    public Long saveAccount(MpAccount input) {
        if (input.getAccountName() == null || input.getAppId() == null) {
            throw BusinessException.of(ErrorCode.BAD_REQUEST, "公众号名称与 AppID 必填");
        }
        Long tenantId = TenantContext.requireTenantId();
        input.setTenantId(tenantId);
        Long dup = accountMapper.selectCount(Wrappers.<MpAccount>lambdaQuery()
                .eq(MpAccount::getAppId, input.getAppId())
                .ne(input.getId() != null, MpAccount::getId, input.getId()));
        if (dup > 0) {
            throw BusinessException.of(ErrorCode.CONFLICT, "该 AppID 已被其他公众号占用");
        }
        keepSecret(input, "appSecret", input.getAppSecret());
        keepSecret(input, "token", input.getToken());
        keepSecret(input, "aesKey", input.getAesKey());
        if (input.getServerUrl() == null || input.getServerUrl().isBlank()) {
            input.setServerUrl("/api/open/mp/callback/" + input.getAppId());
        }
        if (input.getId() == null) {
            accountMapper.insert(input);
        } else {
            MpAccount exist = accountMapper.selectById(input.getId());
            if (exist == null) {
                throw BusinessException.of(ErrorCode.NOT_FOUND, "公众号不存在");
            }
            if (isBlankSecret(input.getAppSecret())) {
                input.setAppSecret(exist.getAppSecret());
            }
            if (isBlankSecret(input.getToken())) {
                input.setToken(exist.getToken());
            }
            if (isBlankSecret(input.getAesKey())) {
                input.setAesKey(exist.getAesKey());
            }
            accountMapper.updateById(input);
        }
        return input.getId();
    }

    @Transactional
    public void deleteAccount(Long id) {
        accountMapper.deleteById(id);
    }

    /** 生成带参二维码（临时二维码，30 天有效）。 */
    public Map<String, Object> qrcode(Long id, String scene) {
        MpAccount account = requireAccount(id);
        if (!client.configured(account)) {
            return Map.of("mode", "LOCAL", "ticket", "local-" + id,
                    "url", "/files/qrcode-placeholder.png", "scene", scene);
        }
        Map<String, Object> body = Map.of(
                "expire_seconds", 2592000,
                "action_name", "QR_STR_SCENE",
                "action_info", Map.of("scene", Map.of("scene_str", scene == null ? "default" : scene)));
        JsonNode node = client.apiPost(account, "/cgi-bin/qrcode/create", body);
        if (node.path("errcode").asInt(0) != 0) {
            throw BusinessException.of(ErrorCode.INTERNAL_ERROR,
                    "生成二维码失败：" + node.path("errmsg").asText());
        }
        return Map.of("ticket", node.path("ticket").asText(),
                "url", "https://mp.weixin.qq.com/cgi-bin/showqrcode?ticket="
                        + java.net.URLEncoder.encode(node.path("ticket").asText(),
                        java.nio.charset.StandardCharsets.UTF_8),
                "expireSeconds", 2592000);
    }

    /** 清空接口调用配额（每月 10 次，慎用）。 */
    public void clearQuota(Long id) {
        MpAccount account = requireAccount(id);
        if (!client.configured(account)) {
            return;
        }
        JsonNode node = client.apiPost(account, "/cgi-bin/clear_quota",
                Map.of("appid", account.getAppId()));
        if (node.path("errcode").asInt(0) != 0) {
            throw BusinessException.of(ErrorCode.INTERNAL_ERROR,
                    "清空配额失败：" + node.path("errmsg").asText());
        }
    }

    /* ---------------- 标签（T65） ---------------- */

    public List<MpTag> tags(Long accountId) {
        return tagMapper.selectList(Wrappers.<MpTag>lambdaQuery()
                .eq(MpTag::getAccountId, accountId).orderByAsc(MpTag::getId));
    }

    @Transactional
    public Long createTag(Long accountId, String name) {
        if (name == null || name.isBlank()) {
            throw BusinessException.of(ErrorCode.BAD_REQUEST, "标签名称必填");
        }
        Long dup = tagMapper.selectCount(Wrappers.<MpTag>lambdaQuery()
                .eq(MpTag::getAccountId, accountId).eq(MpTag::getTagName, name));
        if (dup > 0) {
            throw BusinessException.of(ErrorCode.CONFLICT, "标签名称已存在");
        }
        long total = tagMapper.selectCount(Wrappers.<MpTag>lambdaQuery().eq(MpTag::getAccountId, accountId));
        if (total >= 100) {
            throw BusinessException.of(ErrorCode.CONFLICT, "标签数量已达上限 100 个");
        }
        MpTag tag = new MpTag();
        tag.setTenantId(TenantContext.requireTenantId());
        tag.setAccountId(accountId);
        tag.setTagName(name);
        tag.setFanCount(0);
        tag.setSyncStatus("待同步");
        tagMapper.insert(tag);

        MpAccount account = requireAccount(accountId);
        if (client.configured(account)) {
            try {
                JsonNode node = client.apiPost(account, "/cgi-bin/tags/create",
                        Map.of("tag", Map.of("name", name)));
                if (node.path("errcode").asInt(0) == 0) {
                    tag.setSyncStatus("已同步");
                } else {
                    tag.setSyncStatus("同步失败");
                    recordMessage(accountId, null, "事件", "公众号",
                            "创建标签失败：" + node.path("errmsg").asText(),
                            node.toString(), node.path("errcode").asText(), node.path("errmsg").asText());
                }
            } catch (Exception e) {
                tag.setSyncStatus("同步失败");
                log.warn("创建标签同步失败 accountId={} name={}", accountId, name, e);
            }
            tagMapper.updateById(tag);
        }
        return tag.getId();
    }

    /** 从微信同步标签列表（覆盖式更新，不删除本地手工标签）。 */
    @Transactional
    public int syncTags(Long accountId) {
        MpAccount account = requireAccount(accountId);
        if (!client.configured(account)) {
            return 0;
        }
        JsonNode node = client.apiGet(account, "/cgi-bin/tags/get");
        JsonNode tags = node.path("tags");
        int count = 0;
        for (JsonNode row : tags) {
            long remoteId = row.path("id").asLong();
            String name = row.path("name").asText();
            MpTag exist = tagMapper.selectOne(Wrappers.<MpTag>lambdaQuery()
                    .eq(MpTag::getAccountId, accountId).eq(MpTag::getTagName, name).last("limit 1"));
            if (exist == null) {
                exist = new MpTag();
                exist.setTenantId(TenantContext.requireTenantId());
                exist.setAccountId(accountId);
                exist.setTagName(name);
                exist.setFanCount(row.path("count").asInt());
                exist.setSyncStatus("已同步");
                tagMapper.insert(exist);
            } else {
                exist.setFanCount(row.path("count").asInt());
                exist.setSyncStatus("已同步");
                tagMapper.updateById(exist);
            }
            count++;
        }
        return count;
    }

    @Transactional
    public void deleteTag(Long tagId) {
        MpTag tag = tagMapper.selectById(tagId);
        if (tag == null) {
            return;
        }
        MpAccount account = requireAccount(tag.getAccountId());
        if (client.configured(account)) {
            JsonNode node = client.apiPost(account, "/cgi-bin/tags/delete",
                    Map.of("tag", Map.of("id", tag.getId())));
            if (node.path("errcode").asInt(0) != 0) {
                throw BusinessException.of(ErrorCode.INTERNAL_ERROR,
                        "删除标签失败：" + node.path("errmsg").asText());
            }
        }
        tagMapper.deleteById(tagId);
    }

    /* ---------------- 粉丝（T66） ---------------- */

    public PageResult<MpFan> fans(Long accountId, String keyword, String subscribeStatus,
                                  int pageNum, int pageSize) {
        Page<MpFan> page = fanMapper.selectPage(new Page<>(pageNum, pageSize),
                Wrappers.<MpFan>lambdaQuery()
                        .eq(MpFan::getAccountId, accountId)
                        .like(keyword != null && !keyword.isBlank(), MpFan::getNickname, keyword)
                        .eq(subscribeStatus != null && !subscribeStatus.isBlank(),
                                MpFan::getSubscribeStatus, subscribeStatus)
                        .orderByDesc(MpFan::getId));
        return PageResult.of(page.getRecords(), page.getTotal(), pageNum, pageSize);
    }

    /**
     * 分页同步粉丝（每轮最多 200 人，避免单次请求超时）。
     */
    @Transactional
    public Map<String, Object> syncFans(Long accountId, int limit) {
        MpAccount account = requireAccount(accountId);
        Map<String, Object> result = new LinkedHashMap<>();
        if (!client.configured(account)) {
            result.put("mode", "LOCAL");
            result.put("synced", 0);
            result.put("message", "未配置公众号凭证，仅展示本地已落库粉丝");
            return result;
        }
        String nextOpenId = "";
        int synced = 0;
        int rounds = 0;
        while (rounds < 10 && synced < limit) {
            JsonNode list = client.apiGet(account, "/cgi-bin/user/get?next_openid=" + nextOpenId);
            JsonNode ids = list.path("data").path("openid");
            if (ids.isEmpty()) {
                break;
            }
            for (JsonNode idNode : ids) {
                String openId = idNode.asText();
                JsonNode info = client.apiGet(account, "/cgi-bin/user/info?openid=" + openId + "&lang=zh_CN");
                upsertFan(accountId, info);
                synced++;
                if (synced >= limit) {
                    break;
                }
            }
            nextOpenId = list.path("next_openid").asText("");
            rounds++;
        }
        result.put("mode", "REMOTE");
        result.put("synced", synced);
        result.put("rounds", rounds);
        return result;
    }

    @Transactional
    public void updateFanRemark(Long fanId, String remark) {
        MpFan fan = fanMapper.selectById(fanId);
        if (fan == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "粉丝不存在");
        }
        fan.setRemark(remark);
        fanMapper.updateById(fan);
        MpAccount account = requireAccount(fan.getAccountId());
        if (client.configured(account)) {
            client.apiPost(account, "/cgi-bin/user/info/updateremark",
                    Map.of("openid", fan.getOpenId(), "remark", remark == null ? "" : remark));
        }
    }

    @Transactional
    public void tagFan(Long fanId, Long tagId) {
        MpFan fan = fanMapper.selectById(fanId);
        if (fan == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "粉丝不存在");
        }
        MpAccount account = requireAccount(fan.getAccountId());
        if (client.configured(account)) {
            client.apiPost(account, "/cgi-bin/tags/members/batchtagging",
                    Map.of("openid_list", List.of(fan.getOpenId()), "tagid", tagId));
        }
    }

    /* ---------------- 消息（T67） ---------------- */

    public PageResult<MpMessage> messages(Long accountId, String msgType, LocalDate from, LocalDate to,
                                          int pageNum, int pageSize) {
        Page<MpMessage> page = messageMapper.selectPage(new Page<>(pageNum, pageSize),
                Wrappers.<MpMessage>lambdaQuery()
                        .eq(accountId != null, MpMessage::getAccountId, accountId)
                        .eq(msgType != null && !msgType.isBlank(), MpMessage::getMsgType, msgType)
                        .ge(from != null, MpMessage::getCreatedTime, from == null ? null : from.atStartOfDay())
                        .lt(to != null, MpMessage::getCreatedTime, to == null ? null : to.plusDays(1).atStartOfDay())
                        .orderByDesc(MpMessage::getId));
        return PageResult.of(page.getRecords(), page.getTotal(), pageNum, pageSize);
    }

    /** 关注/取关与普通消息落库，供消息管理与粉丝状态更新使用。 */
    @Transactional
    public void receive(String appId, String openId, String msgType, String content,
                        String rawXml, String event) {
        MpAccount account = accountMapper.selectOne(Wrappers.<MpAccount>lambdaQuery()
                .eq(MpAccount::getAppId, appId).last("limit 1"));
        if (account == null) {
            log.warn("收到未知公众号消息 appId={}", appId);
            return;
        }
        recordMessage(account.getId(), openId, msgType, "用户", content, rawXml, null, null);
        if ("subscribe".equalsIgnoreCase(event)) {
            upsertSubscribe(account.getId(), openId, true);
        } else if ("unsubscribe".equalsIgnoreCase(event)) {
            upsertSubscribe(account.getId(), openId, false);
        }
    }

    /** 关键词自动回复：完全匹配优先，其次半匹配。 */
    public String autoReply(Long accountId, String content) {
        List<MpAutoReply> replies = replyMapper.selectList(Wrappers.<MpAutoReply>lambdaQuery()
                .eq(MpAutoReply::getAccountId, accountId)
                .eq(MpAutoReply::getReplyType, "关键词回复"));
        MpAutoReply matched = null;
        for (MpAutoReply reply : replies) {
            if (reply.getKeyword() == null || reply.getKeyword().isBlank()) {
                continue;
            }
            if ("完全匹配".equals(reply.getMatchType()) && reply.getKeyword().equals(content)) {
                return reply.getContent();
            }
            if (!"完全匹配".equals(reply.getMatchType()) && content != null
                    && content.contains(reply.getKeyword())) {
                matched = reply;
            }
        }
        return matched == null ? null : matched.getContent();
    }

    /* ---------------- 菜单（T69） ---------------- */

    public List<MpMenuConfig> menus(Long accountId) {
        return menuMapper.selectList(Wrappers.<MpMenuConfig>lambdaQuery()
                .eq(MpMenuConfig::getAccountId, accountId).orderByDesc(MpMenuConfig::getId));
    }

    @Transactional
    public Long saveMenu(MpMenuConfig input) {
        validateMenuTree(input.getTreeJson());
        input.setTenantId(TenantContext.requireTenantId());
        if (input.getPublished() == null) {
            input.setPublished(0);
        }
        if (input.getId() == null) {
            menuMapper.insert(input);
        } else {
            menuMapper.updateById(input);
        }
        return input.getId();
    }

    /** 发布菜单：成功后标记已发布；失败保留旧菜单（微信侧未变更）。 */
    public Map<String, Object> publishMenu(Long menuId) {
        MpMenuConfig menu = menuMapper.selectById(menuId);
        if (menu == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "菜单配置不存在");
        }
        MpAccount account = requireAccount(menu.getAccountId());
        if (!client.configured(account)) {
            menu.setPublished(1);
            menu.setPublishTime(LocalDateTime.now());
            menuMapper.updateById(menu);
            return Map.of("mode", "LOCAL", "published", true,
                    "message", "未接号，已在本地标记发布");
        }
        JsonNode node = client.apiPost(account, "/cgi-bin/menu/create",
                objectMapper.convertValue(readTree(menu.getTreeJson()), Map.class));
        int errcode = node.path("errcode").asInt(0);
        if (errcode != 0) {
            recordMessage(account.getId(), null, "事件", "公众号",
                    "发布菜单失败：" + node.path("errmsg").asText(), node.toString(),
                    String.valueOf(errcode), node.path("errmsg").asText());
            throw BusinessException.of(ErrorCode.INTERNAL_ERROR,
                    "菜单发布失败（旧菜单保持生效）：" + node.path("errmsg").asText());
        }
        menu.setPublished(1);
        menu.setPublishTime(LocalDateTime.now());
        menuMapper.updateById(menu);
        return Map.of("mode", "REMOTE", "published", true);
    }

    public void clearMenu(Long accountId) {
        MpAccount account = requireAccount(accountId);
        if (client.configured(account)) {
            JsonNode node = client.apiGet(account, "/cgi-bin/menu/delete");
            if (node.path("errcode").asInt(0) != 0) {
                throw BusinessException.of(ErrorCode.INTERNAL_ERROR,
                        "清空菜单失败：" + node.path("errmsg").asText());
            }
        }
        List<MpMenuConfig> rows = menus(accountId);
        for (MpMenuConfig row : rows) {
            row.setPublished(0);
            row.setPublishTime(null);
            menuMapper.updateById(row);
        }
    }

    /** 模板消息列表（只读，发送与节点映射归消息配置）。 */
    public List<Map<String, Object>> templates(Long accountId) {
        MpAccount account = requireAccount(accountId);
        if (!client.configured(account)) {
            return List.of();
        }
        JsonNode node = client.apiGet(account, "/cgi-bin/template/get_all_private_template");
        List<Map<String, Object>> result = new ArrayList<>();
        for (JsonNode row : node.path("template_list")) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("templateId", row.path("template_id").asText());
            item.put("title", row.path("title").asText());
            item.put("content", row.path("content").asText());
            item.put("example", row.path("example").asText());
            item.put("industry", row.path("primary_industry").asText());
            result.add(item);
        }
        return result;
    }

    /* ---------------- 自动回复 / 素材 / 图文（T70） ---------------- */

    public List<MpAutoReply> replies(Long accountId, String replyType) {
        return replyMapper.selectList(Wrappers.<MpAutoReply>lambdaQuery()
                .eq(MpAutoReply::getAccountId, accountId)
                .eq(replyType != null && !replyType.isBlank(), MpAutoReply::getReplyType, replyType)
                .orderByAsc(MpAutoReply::getId));
    }

    @Transactional
    public Long saveReply(MpAutoReply input) {
        input.setTenantId(TenantContext.requireTenantId());
        if (input.getMsgType() == null) {
            input.setMsgType("文本");
        }
        if (input.getId() == null) {
            replyMapper.insert(input);
        } else {
            replyMapper.updateById(input);
        }
        return input.getId();
    }

    @Transactional
    public void deleteReply(Long id) {
        replyMapper.deleteById(id);
    }

    public List<MpMaterial> materials(Long accountId, String type) {
        return materialMapper.selectList(Wrappers.<MpMaterial>lambdaQuery()
                .eq(accountId != null, MpMaterial::getAccountId, accountId)
                .eq(type != null && !type.isBlank(), MpMaterial::getMaterialType, type)
                .orderByDesc(MpMaterial::getId));
    }

    /**
     * 素材上传：先校验大小限制（图片 ≤2MB、语音 ≤60 秒、视频需缩略图），
     * 落对象存储后再登记台账；已接号时同步到微信永久素材。
     */
    @Transactional
    public MpMaterial uploadMaterial(Long accountId, String name, String type, long sizeBytes,
                                     String fileKey, String thumbMediaId) {
        long limit = switch (type == null ? "图片" : type) {
            case "图片" -> 2 * 1024 * 1024L;
            case "语音" -> 2 * 1024 * 1024L;
            case "视频" -> 10 * 1024 * 1024L;
            default -> 2 * 1024 * 1024L;
        };
        if (sizeBytes > limit) {
            throw BusinessException.of(ErrorCode.BAD_REQUEST,
                    type + " 素材不能超过 " + (limit / 1024 / 1024) + "MB");
        }
        MpMaterial material = new MpMaterial();
        material.setTenantId(TenantContext.requireTenantId());
        material.setAccountId(accountId);
        material.setName(name);
        material.setMaterialType(type);
        material.setSizeBytes(sizeBytes);
        material.setFileKey(fileKey);
        material.setThumbMediaId(thumbMediaId);
        material.setPermanent(1);
        material.setMediaId("LOCAL-" + System.currentTimeMillis());
        materialMapper.insert(material);
        return material;
    }

    public List<MpDraft> drafts(Long accountId, String status) {
        return draftMapper.selectList(Wrappers.<MpDraft>lambdaQuery()
                .eq(accountId != null, MpDraft::getAccountId, accountId)
                .eq(status != null && !status.isBlank(), MpDraft::getStatus, status)
                .orderByAsc(MpDraft::getArticleIndex).orderByDesc(MpDraft::getId));
    }

    @Transactional
    public Long saveDraft(MpDraft input) {
        if (input.getTitle() == null || input.getTitle().isBlank()) {
            throw BusinessException.of(ErrorCode.BAD_REQUEST, "标题必填");
        }
        input.setTenantId(TenantContext.requireTenantId());
        if (input.getStatus() == null) {
            input.setStatus("草稿");
        }
        if (input.getArticleIndex() == null) {
            input.setArticleIndex(1);
        }
        if (input.getId() == null) {
            draftMapper.insert(input);
        } else {
            draftMapper.updateById(input);
        }
        return input.getId();
    }

    /**
     * 发表图文：已接号时提交微信发表接口，失败原因原样记录；未接号时标记为本地草稿已发表。
     */
    @Transactional
    public Map<String, Object> publishDraft(Long draftId) {
        MpDraft draft = draftMapper.selectById(draftId);
        if (draft == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "图文不存在");
        }
        MpAccount account = requireAccount(draft.getAccountId());
        draft.setPublishTime(LocalDateTime.now());
        if (!client.configured(account)) {
            draft.setStatus("已发表");
            draft.setPublishResult("成功");
            draft.setFailReason(null);
            draftMapper.updateById(draft);
            return Map.of("mode", "LOCAL", "success", true);
        }
        try {
            Map<String, Object> article = new LinkedHashMap<>();
            article.put("title", draft.getTitle());
            article.put("author", draft.getAuthor());
            article.put("digest", draft.getDigest());
            article.put("content", draft.getBodyHtml());
            article.put("content_source_url", draft.getSourceUrl());
            article.put("thumb_media_id", draft.getCoverKey());
            article.put("need_open_comment", 0);
            article.put("only_fans_can_comment", 0);
            JsonNode add = client.apiPost(account, "/cgi-bin/draft/add",
                    Map.of("articles", List.of(article)));
            int errcode = add.path("errcode").asInt(0);
            if (errcode != 0) {
                draft.setStatus("草稿");
                draft.setPublishResult("失败");
                draft.setFailReason(add.path("errmsg").asText());
                draftMapper.updateById(draft);
                throw BusinessException.of(ErrorCode.INTERNAL_ERROR,
                        "图文发表失败：" + add.path("errmsg").asText());
            }
            String mediaId = add.path("media_id").asText();
            draft.setDraftMediaId(mediaId);
            draft.setStatus("已发表");
            draft.setPublishResult("审核中");
            draftMapper.updateById(draft);
            return Map.of("mode", "REMOTE", "success", true, "mediaId", mediaId);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            draft.setPublishResult("失败");
            draft.setFailReason(e.getMessage());
            draftMapper.updateById(draft);
            throw BusinessException.of(ErrorCode.INTERNAL_ERROR, "图文发表失败：" + e.getMessage());
        }
    }

    /* ---------------- 数据统计（T64） ---------------- */

    /**
     * 4 张固定图。已接号走微信 datacube，未接号用本地数据生成，返回 simulated 标记。
     */
    public Map<String, Object> stats(Long accountId, LocalDate from, LocalDate to) {
        MpAccount account = requireAccount(accountId);
        LocalDate end = to == null ? LocalDate.now() : to;
        LocalDate start = from == null ? end.minusDays(6) : from;
        if (start.plusDays(31).isBefore(end)) {
            throw BusinessException.of(ErrorCode.BAD_REQUEST, "统计区间最长 31 天");
        }
        String cacheKey = "mp:stats:" + accountId + ":" + start + ":" + end;
        String cached = cacheStore.get(cacheKey);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, Map.class);
            } catch (Exception ignored) {
                // 缓存损坏时重新拉取
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        boolean remote = client.configured(account);
        result.put("simulated", !remote);
        result.put("from", start.toString());
        result.put("to", end.toString());
        if (remote) {
            try {
                Map<String, Object> body = Map.of("begin_date", start.format(DATE),
                        "end_date", end.format(DATE));
                result.put("userSummary", client.apiPost(account, "/datacube/getusersummary", body)
                        .path("list"));
                result.put("userCumulative", client.apiPost(account, "/datacube/getusercumulative", body)
                        .path("list"));
                result.put("messageSummary", client.apiPost(account, "/datacube/getupstreammsg", body)
                        .path("list"));
                result.put("interfaceSummary", client.apiPost(account, "/datacube/getinterfacesummary", body)
                        .path("list"));
            } catch (Exception e) {
                log.warn("拉取公众号统计失败，降级本地数据 accountId={}", accountId, e);
                result.put("simulated", true);
            }
        }
        if (Boolean.TRUE.equals(result.get("simulated"))) {
            result.put("userSummary", localUserSummary(accountId, start, end));
            result.put("userCumulative", localCumulative(accountId, start, end));
            result.put("messageSummary", localMessageSummary(accountId, start, end));
            result.put("interfaceSummary", List.of());
        }
        try {
            cacheStore.put(cacheKey, objectMapper.writeValueAsString(result), Duration.ofMinutes(10));
        } catch (Exception ignored) {
            // 缓存失败不影响返回
        }
        return result;
    }

    /* ---------------- 内部 ---------------- */

    private void upsertFan(Long accountId, JsonNode info) {
        String openId = info.path("openid").asText();
        if (openId.isBlank()) {
            return;
        }
        MpFan fan = fanMapper.selectOne(Wrappers.<MpFan>lambdaQuery()
                .eq(MpFan::getAccountId, accountId).eq(MpFan::getOpenId, openId).last("limit 1"));
        if (fan == null) {
            MpAccount owner = requireAccount(accountId);
            fan = new MpFan();
            fan.setTenantId(owner.getTenantId());
            fan.setAccountId(accountId);
            fan.setOpenId(openId);
        }
        fan.setUnionId(info.path("unionid").asText(null));
        fan.setNickname(info.path("nickname").asText(""));
        fan.setAvatar(info.path("headimgurl").asText(""));
        fan.setGender(switch (info.path("sex").asInt(0)) {
            case 1 -> "男";
            case 2 -> "女";
            default -> "未知";
        });
        fan.setRegion(info.path("country").asText("") + info.path("province").asText("")
                + info.path("city").asText(""));
        fan.setSubscribeStatus(info.path("subscribe").asInt(0) == 1 ? "已关注" : "已取消关注");
        long subscribeTime = info.path("subscribe_time").asLong(0);
        if (subscribeTime > 0) {
            fan.setSubscribeTime(LocalDateTime.ofEpochSecond(subscribeTime, 0,
                    java.time.ZoneOffset.ofHours(8)));
        }
        if (fan.getId() == null) {
            fanMapper.insert(fan);
        } else {
            fanMapper.updateById(fan);
        }
    }

    private void upsertSubscribe(Long accountId, String openId, boolean subscribed) {
        MpFan fan = fanMapper.selectOne(Wrappers.<MpFan>lambdaQuery()
                .eq(MpFan::getAccountId, accountId).eq(MpFan::getOpenId, openId).last("limit 1"));
        if (fan == null) {
            MpAccount owner = requireAccount(accountId);
            fan = new MpFan();
            fan.setTenantId(owner.getTenantId());
            fan.setAccountId(accountId);
            fan.setOpenId(openId);
            fan.setSubscribeStatus(subscribed ? "已关注" : "已取消关注");
            fan.setSubscribeTime(LocalDateTime.now());
            fanMapper.insert(fan);
            return;
        }
        fan.setSubscribeStatus(subscribed ? "已关注" : "已取消关注");
        if (subscribed) {
            fan.setSubscribeTime(LocalDateTime.now());
            fan.setUnsubscribeTime(null);
        } else {
            fan.setUnsubscribeTime(LocalDateTime.now());
        }
        fanMapper.updateById(fan);
    }

    private void recordMessage(Long accountId, String openId, String msgType, String sender,
                               String content, String raw, String errorCode, String errorMsg) {
        MpAccount owner = accountId == null ? null : accountMapper.selectById(accountId);
        Long tenantId = owner != null ? owner.getTenantId() : TenantContext.getTenantId();
        MpMessage message = new MpMessage();
        message.setTenantId(tenantId == null ? -1L : tenantId);
        message.setAccountId(accountId);
        message.setOpenId(openId);
        message.setMsgType(msgType);
        message.setSender(sender);
        message.setContent(content);
        message.setRawPayload(raw);
        message.setErrorCode(errorCode);
        message.setErrorMsg(errorMsg);
        messageMapper.insert(message);
    }

    private List<Map<String, Object>> localUserSummary(Long accountId, LocalDate start, LocalDate end) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            LocalDate day = d;
            long added = fanMapper.selectCount(Wrappers.<MpFan>lambdaQuery()
                    .eq(MpFan::getAccountId, accountId)
                    .ge(MpFan::getSubscribeTime, day.atStartOfDay())
                    .lt(MpFan::getSubscribeTime, day.plusDays(1).atStartOfDay()));
            long lost = fanMapper.selectCount(Wrappers.<MpFan>lambdaQuery()
                    .eq(MpFan::getAccountId, accountId)
                    .ge(MpFan::getUnsubscribeTime, day.atStartOfDay())
                    .lt(MpFan::getUnsubscribeTime, day.plusDays(1).atStartOfDay()));
            rows.add(Map.of("date", day.toString(), "newUser", added, "cancelUser", lost));
        }
        return rows;
    }

    private List<Map<String, Object>> localCumulative(Long accountId, LocalDate start, LocalDate end) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            long total = fanMapper.selectCount(Wrappers.<MpFan>lambdaQuery()
                    .eq(MpFan::getAccountId, accountId)
                    .eq(MpFan::getSubscribeStatus, "已关注"));
            rows.add(Map.of("date", d.toString(), "cumulativeUser", total));
        }
        return rows;
    }

    private List<Map<String, Object>> localMessageSummary(Long accountId, LocalDate start, LocalDate end) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            long count = messageMapper.selectCount(Wrappers.<MpMessage>lambdaQuery()
                    .eq(MpMessage::getAccountId, accountId)
                    .ge(MpMessage::getCreatedTime, d.atStartOfDay())
                    .lt(MpMessage::getCreatedTime, d.plusDays(1).atStartOfDay()));
            rows.add(Map.of("date", d.toString(), "msgCount", count));
        }
        return rows;
    }

    private void validateMenuTree(String treeJson) {
        try {
            JsonNode tree = readTree(treeJson);
            JsonNode buttons = tree.has("button") ? tree.path("button") : tree;
            if (!buttons.isArray() || buttons.size() > 3) {
                throw BusinessException.of(ErrorCode.BAD_REQUEST, "公众号一级菜单最多 3 个");
            }
            for (JsonNode button : buttons) {
                JsonNode sub = button.path("sub_button");
                if (sub.isArray() && sub.size() > 5) {
                    throw BusinessException.of(ErrorCode.BAD_REQUEST, "公众号二级菜单最多 5 个");
                }
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw BusinessException.of(ErrorCode.BAD_REQUEST, "菜单树不是合法 JSON");
        }
    }

    private JsonNode readTree(String json) {
        try {
            return objectMapper.readTree(json == null ? "{}" : json);
        } catch (Exception e) {
            throw BusinessException.of(ErrorCode.BAD_REQUEST, "JSON 格式不合法");
        }
    }

    private void keepSecret(MpAccount input, String field, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        String encrypted = secretCipher.encrypt(value);
        switch (field) {
            case "appSecret" -> input.setAppSecret(encrypted);
            case "token" -> input.setToken(encrypted);
            case "aesKey" -> input.setAesKey(encrypted);
            default -> {
                // 无其他凭证字段
            }
        }
    }

    private boolean isBlankSecret(String value) {
        return value == null || value.isBlank() || DesensitizeUtil.isMasked(value);
    }

    private void maskAccount(MpAccount account) {
        account.setAppSecret(DesensitizeUtil.secret(account.getAppSecret()));
        account.setToken(DesensitizeUtil.secret(account.getToken()));
        account.setAesKey(DesensitizeUtil.secret(account.getAesKey()));
    }

    public MpAccount requireAccount(Long id) {
        MpAccount account = accountMapper.selectById(id);
        if (account == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "公众号不存在");
        }
        return account;
    }
}
