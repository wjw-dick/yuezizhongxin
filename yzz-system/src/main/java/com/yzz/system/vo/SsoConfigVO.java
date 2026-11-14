package com.yzz.system.vo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * SSO 配置回显。授权、令牌、校验地址由服务端按协议自动拼接，前端只填根地址。
 */
public record SsoConfigVO(
        boolean enabled,
        String protocol,
        String serverUrl,
        String serviceUrl,
        String clientId,
        String clientSecretMasked,
        String accountMapping,
        boolean fallbackEnabled,
        String rejectStrategy,
        List<Endpoint> endpoints,
        LocalDateTime updatedTime) {

    public record Endpoint(String name, String url) {
    }
}
