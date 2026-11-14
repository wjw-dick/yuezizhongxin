package com.yzz.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yzz.common.api.BusinessException;
import com.yzz.common.api.ErrorCode;
import com.yzz.common.tenant.TenantContext;
import com.yzz.common.util.SecretCipher;
import com.yzz.system.dto.SsoConfigRequest;
import com.yzz.system.entity.SysSsoConfig;
import com.yzz.system.mapper.SysSsoConfigMapper;
import com.yzz.system.vo.SsoConfigVO;
import com.yzz.tenant.entity.PlatformTenant;
import com.yzz.tenant.mapper.PlatformTenantMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * SSO 配置（T28）。
 *
 * <p>四种协议：CAS / OAuth2 / OIDC / Sa-Token。运营只填服务器根地址，
 * 授权、令牌、校验地址由服务端按协议拼接；Secret 加密落库、接口只回掩码；
 * SSO 故障时可按配置降级为账号密码登录。</p>
 */
@Service
public class SsoService {

    private static final Logger log = LoggerFactory.getLogger(SsoService.class);
    private static final List<String> PROTOCOLS = List.of("CAS", "OAuth2", "OIDC", "Sa-Token");

    private final SysSsoConfigMapper ssoMapper;
    private final SecretCipher secretCipher;
    private final PlatformTenantMapper tenantMapper;

    public SsoService(SysSsoConfigMapper ssoMapper, SecretCipher secretCipher,
                      PlatformTenantMapper tenantMapper) {
        this.ssoMapper = ssoMapper;
        this.secretCipher = secretCipher;
        this.tenantMapper = tenantMapper;
    }

    public SsoConfigVO current() {
        SysSsoConfig row = findRow();
        if (row == null) {
            return new SsoConfigVO(false, "OIDC", null, null, null, null, null,
                    true, "拒绝", List.of(), null);
        }
        return toVO(row);
    }

    public SsoConfigVO save(SsoConfigRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        if (!PROTOCOLS.contains(request.protocol())) {
            throw BusinessException.of(ErrorCode.BAD_REQUEST, "不支持的 SSO 协议：" + request.protocol());
        }
        String root = normalizeRoot(request.serverUrl());
        SysSsoConfig row = findRow();
        String secret = request.clientSecret();
        if (secret == null || secret.isBlank()) {
            secret = row == null ? null : row.getClientSecret();
        }
        if (request.enabled() && (secret == null || secret.isBlank())) {
            throw BusinessException.of(ErrorCode.BAD_REQUEST, "启用 SSO 前请填写客户端密钥");
        }
        if (request.enabled() && (request.clientId() == null || request.clientId().isBlank())) {
            throw BusinessException.of(ErrorCode.BAD_REQUEST, "启用 SSO 前请填写客户端标识");
        }

        if (row == null) {
            row = new SysSsoConfig();
            row.setTenantId(tenantId);
        }
        row.setEnabled(request.enabled() ? 1 : 0);
        row.setProtocol(request.protocol());
        row.setServerUrl(root);
        row.setServiceUrl(request.serviceUrl());
        row.setClientId(request.clientId());
        row.setClientSecret(secret == null || secret.isBlank() ? null : secretCipher.encrypt(secret));
        row.setAccountMapping(request.accountMapping() == null || request.accountMapping().isBlank()
                ? "username" : request.accountMapping());
        row.setFallbackEnabled(request.fallbackEnabled() == null || request.fallbackEnabled() ? 1 : 0);
        row.setRejectStrategy(request.rejectStrategy() == null || request.rejectStrategy().isBlank()
                ? "拒绝" : request.rejectStrategy());
        if (row.getId() == null) {
            ssoMapper.insert(row);
        } else {
            ssoMapper.updateById(row);
        }
        log.info("SSO 配置已保存 tenantId={} protocol={} enabled={}",
                tenantId, request.protocol(), request.enabled());
        return toVO(row);
    }

    /** 登录页读取：是否启用 SSO、启用时返回授权入口地址。 */
    public SsoLoginInfo loginInfo() {
        SysSsoConfig row = findRow();
        if (row == null || row.getEnabled() == null || row.getEnabled() == 0) {
            return new SsoLoginInfo(false, null, true);
        }
        String authUrl = null;
        for (SsoConfigVO.Endpoint endpoint : endpointsOf(row.getProtocol(), row.getServerUrl(),
                row.getServiceUrl())) {
            if (endpoint.name().contains("授权") || endpoint.name().contains("登录")) {
                authUrl = endpoint.url();
                break;
            }
        }
        return new SsoLoginInfo(true, authUrl,
                row.getFallbackEnabled() != null && row.getFallbackEnabled() == 1);
    }

    private SsoConfigVO toVO(SysSsoConfig row) {
        String secret = row.getClientSecret();
        String masked = null;
        if (secret != null && !secret.isBlank()) {
            try {
                String plain = secretCipher.decrypt(secret);
                masked = plain.length() <= 8 ? "****"
                        : plain.substring(0, 4) + "****" + plain.substring(plain.length() - 4);
            } catch (RuntimeException e) {
                masked = "****";
            }
        }
        return new SsoConfigVO(row.getEnabled() != null && row.getEnabled() == 1, row.getProtocol(),
                row.getServerUrl(), row.getServiceUrl(), row.getClientId(), masked,
                row.getAccountMapping(),
                row.getFallbackEnabled() == null || row.getFallbackEnabled() == 1,
                row.getRejectStrategy(),
                endpointsOf(row.getProtocol(), row.getServerUrl(), row.getServiceUrl()),
                row.getUpdatedTime());
    }

    private List<SsoConfigVO.Endpoint> endpointsOf(String protocol, String root, String serviceUrl) {
        List<SsoConfigVO.Endpoint> list = new ArrayList<>();
        if (root == null || root.isBlank()) {
            return list;
        }
        String base = normalizeRoot(root);
        switch (protocol == null ? "OIDC" : protocol) {
            case "CAS" -> {
                list.add(new SsoConfigVO.Endpoint("登录地址",
                        base + "/login" + (serviceUrl == null ? "" : "?service=" + serviceUrl)));
                list.add(new SsoConfigVO.Endpoint("票据校验地址", base + "/serviceValidate"));
                list.add(new SsoConfigVO.Endpoint("登出地址", base + "/logout"));
            }
            case "OAuth2" -> {
                list.add(new SsoConfigVO.Endpoint("授权地址", base + "/oauth/authorize"));
                list.add(new SsoConfigVO.Endpoint("令牌地址", base + "/oauth/token"));
                list.add(new SsoConfigVO.Endpoint("用户信息地址", base + "/userinfo"));
            }
            case "Sa-Token" -> {
                list.add(new SsoConfigVO.Endpoint("登录地址", base + "/sso/auth"));
                list.add(new SsoConfigVO.Endpoint("令牌地址", base + "/sso/token"));
                list.add(new SsoConfigVO.Endpoint("登出地址", base + "/sso/logout"));
            }
            default -> {
                list.add(new SsoConfigVO.Endpoint("发现文档", base + "/.well-known/openid-configuration"));
                list.add(new SsoConfigVO.Endpoint("授权地址", base + "/authorize"));
                list.add(new SsoConfigVO.Endpoint("令牌地址", base + "/token"));
            }
        }
        return list;
    }

    /**
     * 只保留根地址：若运营粘贴了完整授权地址，自动裁掉协议后缀，避免出现双路径。
     */
    static String normalizeRoot(String serverUrl) {
        if (serverUrl == null) {
            return null;
        }
        String value = serverUrl.trim().replaceAll("/+$", "");
        String[] suffixes = {
                "/.well-known/openid-configuration", "/oauth/authorize", "/oauth/token",
                "/serviceValidate", "/sso/auth", "/sso/token", "/sso/logout", "/authorize",
                "/token", "/userinfo", "/login", "/logout"};
        for (String suffix : suffixes) {
            if (value.endsWith(suffix)) {
                value = value.substring(0, value.length() - suffix.length());
                break;
            }
        }
        return value.replaceAll("/+$", "");
    }

    private SysSsoConfig findRow() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            // 登录页（/api/open）没有登录态，只有子域名或请求头里的租户编码，按编码反查租户
            String tenantCode = TenantContext.getTenantCode();
            if (tenantCode == null || tenantCode.isBlank()) {
                return null;
            }
            PlatformTenant tenant = tenantMapper.selectOne(
                    com.baomidou.mybatisplus.core.toolkit.Wrappers.<PlatformTenant>lambdaQuery()
                            .eq(PlatformTenant::getTenantCode, tenantCode).last("limit 1"));
            if (tenant == null) {
                return null;
            }
            tenantId = tenant.getId();
            // 回填租户上下文，否则租户插件会给后续查询注入 tenant_id = -1
            TenantContext.initTenant(tenantId, tenantCode);
        }
        return ssoMapper.selectOne(Wrappers.<SysSsoConfig>lambdaQuery()
                .eq(SysSsoConfig::getTenantId, tenantId).last("limit 1"));
    }

    /** 登录页所需的最小信息。 */
    public record SsoLoginInfo(boolean enabled, String authUrl, boolean passwordFallback) {
    }
}
