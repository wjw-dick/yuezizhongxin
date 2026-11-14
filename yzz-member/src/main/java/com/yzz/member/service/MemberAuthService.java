package com.yzz.member.service;

import com.yzz.common.api.BusinessException;
import com.yzz.common.api.ErrorCode;
import com.yzz.common.guard.CacheStore;
import com.yzz.common.tenant.TenantContext;
import com.yzz.common.util.TokenUtil;
import com.yzz.member.entity.MemberInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

/**
 * C 端（小程序 / H5 / App）会员登录（T74、T85）。
 *
 * <p>手机号 + 短信验证码登录，未建档自动建会员。令牌复用 {@link TokenUtil}，
 * 角色固定 MEMBER，载荷里的租户必须与当前请求租户一致，防止跨租户冒用。
 * 未接入短信通道时（yzz.sms.mock=true）验证码回显，便于联调与验收。</p>
 */
@Service
public class MemberAuthService {

    private static final Logger log = LoggerFactory.getLogger(MemberAuthService.class);
    private static final Pattern PHONE = Pattern.compile("^1[3-9]\\d{9}$");
    private static final String ROLE = "MEMBER";

    private final CacheStore cacheStore;
    private final MemberService memberService;
    private final TokenUtil tokenUtil;
    private final boolean mock;

    public MemberAuthService(CacheStore cacheStore, MemberService memberService, TokenUtil tokenUtil,
                             @Value("${yzz.sms.mock:true}") boolean mock) {
        this.cacheStore = cacheStore;
        this.memberService = memberService;
        this.tokenUtil = tokenUtil;
        this.mock = mock;
    }

    /** 发送验证码：同号 60 秒一条，单号每小时 10 条上限。 */
    public Map<String, Object> sendCode(String phone) {
        validatePhone(phone);
        Long tenantId = TenantContext.requireTenantId();
        String limitKey = "member:sms:limit:" + tenantId + ":" + phone;
        if (!cacheStore.setIfAbsent(limitKey, Duration.ofSeconds(60))) {
            throw BusinessException.of(ErrorCode.RATE_LIMITED, "验证码已发送，请 60 秒后再试");
        }
        long hourly = cacheStore.increment("member:sms:hour:" + tenantId + ":" + phone, Duration.ofHours(1));
        if (hourly > 10) {
            throw BusinessException.of(ErrorCode.RATE_LIMITED, "今日验证码发送次数过多，请联系门店顾问");
        }
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
        cacheStore.put(codeKey(tenantId, phone), code, Duration.ofMinutes(5));
        log.info("会员验证码已生成 tenantId={} phone={} mock={}", tenantId, mask(phone), mock);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sent", true);
        result.put("expireSeconds", 300);
        result.put("mock", mock);
        if (mock) {
            // 未接入短信通道时回显，前端以「联调验证码」气泡提示
            result.put("devCode", code);
        }
        return result;
    }

    /** 登录/注册：校验验证码，会员不存在则自动建档。 */
    public Map<String, Object> login(String phone, String code, String source) {
        validatePhone(phone);
        if (code == null || code.isBlank()) {
            throw BusinessException.of(ErrorCode.BAD_REQUEST, "请输入验证码");
        }
        Long tenantId = TenantContext.requireTenantId();
        String key = codeKey(tenantId, phone);
        String cached = cacheStore.get(key);
        if (cached == null) {
            throw BusinessException.of(ErrorCode.BAD_REQUEST, "验证码已过期，请重新获取");
        }
        if (!cached.equals(code.trim())) {
            throw BusinessException.of(ErrorCode.BAD_REQUEST, "验证码不正确");
        }
        cacheStore.delete(key);

        MemberInfo member = memberService.findByPhone(phone);
        boolean created = false;
        if (member == null) {
            member = memberService.createByPhone(phone, null, source);
            created = true;
            log.info("会员首次登录自动建档 tenantId={} memberId={}", tenantId, member.getId());
        }
        String token = tokenUtil.issue(member.getId(), tenantId, TenantContext.getTenantCode(), false, ROLE);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("token", token);
        result.put("created", created);
        result.put("profile", memberService.profile(member.getId()));
        return result;
    }

    /** 令牌换取会员主键；租户不一致直接拒绝。 */
    public Long resolve(String token) {
        if (token == null || token.isBlank()) {
            throw BusinessException.of(ErrorCode.UNAUTHORIZED);
        }
        TokenUtil.Claims claims = tokenUtil.parse(token.replace("Bearer ", "").trim());
        if (!ROLE.equals(claims.roleCode())) {
            throw BusinessException.of(ErrorCode.UNAUTHORIZED, "令牌类型不正确");
        }
        Long tenantId = TenantContext.requireTenantId();
        if (claims.tenantId() != null && !claims.tenantId().equals(tenantId)) {
            throw BusinessException.of(ErrorCode.TENANT_ACCESS_DENIED);
        }
        return claims.userId();
    }

    private void validatePhone(String phone) {
        if (phone == null || !PHONE.matcher(phone.trim()).matches()) {
            throw BusinessException.of(ErrorCode.BAD_REQUEST, "请输入正确的 11 位手机号");
        }
    }

    private String codeKey(Long tenantId, String phone) {
        return "member:sms:code:" + tenantId + ":" + phone.trim();
    }

    private String mask(String phone) {
        return phone.length() == 11 ? phone.substring(0, 3) + "****" + phone.substring(7) : "***";
    }
}
