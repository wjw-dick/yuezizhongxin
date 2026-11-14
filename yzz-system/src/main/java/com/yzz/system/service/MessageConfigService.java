package com.yzz.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yzz.common.api.BusinessException;
import com.yzz.common.api.ErrorCode;
import com.yzz.common.tenant.TenantContext;
import com.yzz.common.util.SecretCipher;
import com.yzz.system.dto.MsgChannelRequest;
import com.yzz.system.dto.MsgTemplateRequest;
import com.yzz.system.entity.MsgChannelConfig;
import com.yzz.system.entity.MsgTemplate;
import com.yzz.system.mapper.MsgChannelConfigMapper;
import com.yzz.system.mapper.MsgTemplateMapper;
import com.yzz.system.vo.MsgChannelVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 消息配置（T27）。
 *
 * <p>通道表同时承载 4 个消息通道与 6 个短信渠道：短信渠道全局单选启用，
 * 消息通道各自独立开关。同节点同通道模板唯一；未启用任何短信渠道时，
 * 验证码发送降级写日志，不阻断业务流程。</p>
 */
@Service
public class MessageConfigService {

    private static final Logger log = LoggerFactory.getLogger(MessageConfigService.class);

    /** 短信渠道编码 → 中文名（页面卡片顺序），6 选 1。 */
    private static final Map<String, String> SMS_CHANNELS = new LinkedHashMap<>();

    /** 消息通道编码 → 中文名（4 张固定卡片）。 */
    private static final Map<String, String> MESSAGE_CHANNELS = new LinkedHashMap<>();

    static {
        SMS_CHANNELS.put("aliyun_sms", "阿里云短信");
        SMS_CHANNELS.put("tencent_sms", "腾讯云短信");
        SMS_CHANNELS.put("huawei_sms", "华为云短信");
        SMS_CHANNELS.put("jd_sms", "京东云短信");
        SMS_CHANNELS.put("aws_sns", "Amazon SNS");
        SMS_CHANNELS.put("local_log", "本地日志（调试用）");

        MESSAGE_CHANNELS.put("mp_template", "公众号模板消息");
        MESSAGE_CHANNELS.put("mini_subscribe", "小程序订阅消息");
        MESSAGE_CHANNELS.put("app_push", "App 推送");
        MESSAGE_CHANNELS.put("site_message", "站内信");
    }

    private final MsgChannelConfigMapper channelMapper;
    private final MsgTemplateMapper templateMapper;
    private final SecretCipher secretCipher;

    public MessageConfigService(MsgChannelConfigMapper channelMapper,
                                MsgTemplateMapper templateMapper, SecretCipher secretCipher) {
        this.channelMapper = channelMapper;
        this.templateMapper = templateMapper;
        this.secretCipher = secretCipher;
    }

    public List<MsgChannelVO> smsChannels() {
        return cards(SMS_CHANNELS);
    }

    public List<MsgChannelVO> messageChannels() {
        return cards(MESSAGE_CHANNELS);
    }

    @Transactional
    public void saveChannel(MsgChannelRequest request) {
        String code = request.channelCode();
        boolean sms = SMS_CHANNELS.containsKey(code);
        if (!sms && !MESSAGE_CHANNELS.containsKey(code)) {
            throw BusinessException.of(ErrorCode.BAD_REQUEST, "不支持的渠道：" + code);
        }
        ensureSeed();
        MsgChannelConfig row = channelMapper.selectOne(Wrappers.<MsgChannelConfig>lambdaQuery()
                .eq(MsgChannelConfig::getChannelCode, code).last("limit 1"));
        if (row == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "渠道不存在：" + code);
        }
        if (sms && request.enabled()) {
            // 短信渠道互斥：启用前先停用其余短信渠道（消息通道不受影响）
            for (MsgChannelConfig other : rows()) {
                if (SMS_CHANNELS.containsKey(other.getChannelCode())
                        && !other.getId().equals(row.getId())
                        && other.getEnabled() != null && other.getEnabled() == 1) {
                    MsgChannelConfig update = new MsgChannelConfig();
                    update.setId(other.getId());
                    update.setEnabled(0);
                    channelMapper.updateById(update);
                }
            }
        }
        if (request.configJson() != null && !request.configJson().isBlank()) {
            // 渠道参数含密钥，整体加密落库，接口只回是否已配置
            row.setConfigJson(secretCipher.encrypt(request.configJson()));
        }
        row.setEnabled(request.enabled() ? 1 : 0);
        channelMapper.updateById(row);
        log.info("渠道配置更新 tenantId={} channel={} enabled={}",
                TenantContext.getTenantId(), code, request.enabled());
    }

    /**
     * 测试发送。仅本地日志渠道可直接验证；真实渠道需接入网关凭证。
     */
    public String testSend(String channelCode, String phone, String content) {
        MsgChannelConfig row = requireEnabled(channelCode);
        String text = content == null || content.isBlank() ? "【月子中心】这是一条测试短信" : content;
        if ("local_log".equals(row.getChannelCode())) {
            log.info("[SMS-LOCAL] phone={} content={}", phone, text);
            return "已写入本地日志（调试渠道不真实发送）";
        }
        throw BusinessException.of(ErrorCode.BAD_REQUEST,
                "渠道「" + row.getChannelName() + "」尚未接入真实网关，请核对凭证后由运维启用");
    }

    /**
     * 验证码发送。未启用渠道时降级写日志并返回 false，调用方继续走后续流程。
     */
    public boolean sendVerificationCode(String phone, String code) {
        MsgChannelConfig enabled = enabledSms();
        if (enabled == null) {
            log.warn("[SMS-FALLBACK] 未启用短信渠道，验证码仅写日志 phone={} code={}", phone, code);
            return false;
        }
        if ("local_log".equals(enabled.getChannelCode())) {
            log.info("[SMS-LOCAL] phone={} 验证码={}", phone, code);
            return true;
        }
        log.warn("[SMS] 渠道 {} 未接入网关，验证码降级写日志 phone={}", enabled.getChannelCode(), phone);
        return false;
    }

    public List<MsgTemplate> templates(String triggerNode, String channel) {
        return templateMapper.selectList(Wrappers.<MsgTemplate>lambdaQuery()
                .eq(triggerNode != null && !triggerNode.isBlank(), MsgTemplate::getTriggerNode, triggerNode)
                .eq(channel != null && !channel.isBlank(), MsgTemplate::getChannel, channel)
                .orderByAsc(MsgTemplate::getTriggerNode)
                .orderByAsc(MsgTemplate::getChannel));
    }

    @Transactional
    public void saveTemplate(MsgTemplateRequest request) {
        MsgTemplate exists = templateMapper.selectOne(Wrappers.<MsgTemplate>lambdaQuery()
                .eq(MsgTemplate::getTriggerNode, request.triggerNode())
                .eq(MsgTemplate::getChannel, request.channel()).last("limit 1"));
        if (exists != null) {
            exists.setTemplateCode(request.templateCode());
            exists.setFieldMapping(request.fieldMapping());
            templateMapper.updateById(exists);
            return;
        }
        MsgTemplate entity = new MsgTemplate();
        entity.setTenantId(TenantContext.requireTenantId());
        entity.setTriggerNode(request.triggerNode());
        entity.setChannel(request.channel());
        entity.setTemplateCode(request.templateCode());
        entity.setFieldMapping(request.fieldMapping());
        templateMapper.insert(entity);
    }

    public void deleteTemplate(Long id) {
        templateMapper.deleteById(id);
    }

    private List<MsgChannelVO> cards(Map<String, String> catalog) {
        ensureSeed();
        Map<String, MsgChannelConfig> byCode = new LinkedHashMap<>();
        for (MsgChannelConfig row : rows()) {
            byCode.put(row.getChannelCode(), row);
        }
        List<MsgChannelVO> result = new ArrayList<>(catalog.size());
        for (Map.Entry<String, String> entry : catalog.entrySet()) {
            MsgChannelConfig row = byCode.get(entry.getKey());
            boolean enabled = row != null && row.getEnabled() != null && row.getEnabled() == 1;
            boolean configured = row != null && row.getConfigJson() != null
                    && !row.getConfigJson().isBlank();
            result.add(new MsgChannelVO(entry.getKey(), entry.getValue(), enabled, null, configured));
        }
        return result;
    }

    private MsgChannelConfig requireEnabled(String channelCode) {
        MsgChannelConfig row = channelMapper.selectOne(Wrappers.<MsgChannelConfig>lambdaQuery()
                .eq(MsgChannelConfig::getChannelCode, channelCode).last("limit 1"));
        if (row == null || row.getEnabled() == null || row.getEnabled() == 0) {
            throw BusinessException.of(ErrorCode.BAD_REQUEST, "渠道未启用，请先启用后再测试");
        }
        return row;
    }

    private MsgChannelConfig enabledSms() {
        List<MsgChannelConfig> rows = rows();
        for (MsgChannelConfig row : rows) {
            if (SMS_CHANNELS.containsKey(row.getChannelCode())
                    && row.getEnabled() != null && row.getEnabled() == 1) {
                return row;
            }
        }
        return null;
    }

    private List<MsgChannelConfig> rows() {
        return channelMapper.selectList(Wrappers.<MsgChannelConfig>lambdaQuery()
                .orderByAsc(MsgChannelConfig::getId));
    }

    /** 补齐缺失的通道卡片，兼容历史数据只预置部分渠道的租户。 */
    private void ensureSeed() {
        Long tenantId = TenantContext.requireTenantId();
        Map<String, MsgChannelConfig> existing = new LinkedHashMap<>();
        for (MsgChannelConfig row : rows()) {
            existing.put(row.getChannelCode(), row);
        }
        int added = 0;
        for (Map.Entry<String, String> entry : SMS_CHANNELS.entrySet()) {
            added += seedOne(tenantId, entry, existing);
        }
        for (Map.Entry<String, String> entry : MESSAGE_CHANNELS.entrySet()) {
            added += seedOne(tenantId, entry, existing);
        }
        if (added > 0) {
            log.info("补齐消息通道卡片 tenantId={} added={}", tenantId, added);
        }
    }

    private int seedOne(Long tenantId, Map.Entry<String, String> entry,
                        Map<String, MsgChannelConfig> existing) {
        if (existing.containsKey(entry.getKey())) {
            return 0;
        }
        MsgChannelConfig entity = new MsgChannelConfig();
        entity.setTenantId(tenantId);
        entity.setChannelCode(entry.getKey());
        entity.setChannelName(entry.getValue());
        entity.setEnabled(0);
        channelMapper.insert(entity);
        return 1;
    }
}
