package com.yzz.common.util;

import com.yzz.common.api.BusinessException;
import com.yzz.common.api.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenUtilTest {

    private final TokenUtil tokenUtil = new TokenUtil("unit-test-secret-key-32-bytes-long", 60);

    @Test
    void 签发后可解析出原租户与角色() {
        String token = tokenUtil.issue(100L, 1L, "demo", false, "tenant_admin");
        TokenUtil.Claims claims = tokenUtil.parse(token);

        assertEquals(100L, claims.userId());
        assertEquals(1L, claims.tenantId());
        assertEquals("demo", claims.tenantCode());
        assertFalse(claims.platform());
        assertEquals("tenant_admin", claims.roleCode());
    }

    @Test
    void 篡改载荷应被签名校验拦截() {
        String token = tokenUtil.issue(100L, 1L, "demo", false, "tenant_admin");
        String tampered = token.substring(0, token.length() - 3) + "abc";

        BusinessException ex = assertThrows(BusinessException.class, () -> tokenUtil.parse(tampered));
        assertEquals(ErrorCode.UNAUTHORIZED, ex.getErrorCode());
    }

    @Test
    void 过期令牌不可用() {
        TokenUtil expired = new TokenUtil("unit-test-secret-key-32-bytes-long", -1);
        String token = expired.issue(100L, 1L, "demo", false, "tenant_admin");

        BusinessException ex = assertThrows(BusinessException.class, () -> expired.parse(token));
        assertEquals(ErrorCode.UNAUTHORIZED, ex.getErrorCode());
    }

    @Test
    void 平台令牌不带租户标识() {
        String token = tokenUtil.issue(9L, null, null, true, "platform_super");
        TokenUtil.Claims claims = tokenUtil.parse(token);

        assertTrue(claims.platform());
        assertEquals(null, claims.tenantId());
    }
}
