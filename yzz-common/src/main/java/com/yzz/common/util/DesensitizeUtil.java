package com.yzz.common.util;

/**
 * 敏感信息脱敏工具。
 *
 * <p>列表、详情、导出统一调用本类，禁止在业务代码里各写一套截取规则。</p>
 */
public final class DesensitizeUtil {

    private DesensitizeUtil() {
    }

    /** 手机号：保留前 3 位与后 4 位。 */
    public static String phone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /** 身份证：仅保留首 4 位与末 2 位。 */
    public static String idCard(String idCard) {
        if (idCard == null || idCard.length() < 8) {
            return idCard;
        }
        return idCard.substring(0, 4) + "**********" + idCard.substring(idCard.length() - 2);
    }

    /** 姓名：仅保留姓。 */
    public static String name(String name) {
        if (name == null || name.isBlank()) {
            return name;
        }
        return name.charAt(0) + "**";
    }

    /** 密钥类字段：非空即整体遮蔽，页面与接口均不回明文。 */
    public static String secret(String secret) {
        if (secret == null || secret.isBlank()) {
            return "";
        }
        return "******";
    }

    /** 判断前端提交的是否为掩码值（提交掩码表示「留空不改」）。 */
    public static boolean isMasked(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        return value.chars().allMatch(c -> c == '*');
    }

    /** 地址：保留到街道，门牌号掩码。 */
    public static String address(String address) {
        if (address == null || address.length() <= 6) {
            return address;
        }
        return address.substring(0, 6) + "****";
    }
}
