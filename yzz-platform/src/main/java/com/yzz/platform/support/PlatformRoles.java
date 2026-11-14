package com.yzz.platform.support;

/**
 * 平台侧三个角色。与租户侧九个角色完全隔离，权限不互通。
 */
public final class PlatformRoles {

    public static final String SUPER = "platform_super";
    public static final String OPERATOR = "platform_oper";
    public static final String FINANCE = "platform_fin";

    private PlatformRoles() {
    }

    public static String nameOf(String roleCode) {
        if (SUPER.equals(roleCode)) {
            return "平台超级管理员";
        }
        if (OPERATOR.equals(roleCode)) {
            return "平台运营";
        }
        if (FINANCE.equals(roleCode)) {
            return "平台财务";
        }
        return "未知角色";
    }
}
