package com.yzz.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DesensitizeUtilTest {

    @Test
    void 手机号保留前三后四() {
        assertEquals("138****8000", DesensitizeUtil.phone("13800008000"));
    }

    @Test
    void 身份证仅保留首尾() {
        assertEquals("3301**********34", DesensitizeUtil.idCard("330106199001011234"));
    }

    @Test
    void 姓名为空时原样返回() {
        assertEquals(null, DesensitizeUtil.name(null));
        assertEquals("张**", DesensitizeUtil.name("张三"));
    }

    @Test
    void 掩码值判定用于留空不改() {
        assertTrue(DesensitizeUtil.isMasked("******"));
        assertFalse(DesensitizeUtil.isMasked("abc123"));
        assertFalse(DesensitizeUtil.isMasked(""));
    }
}
