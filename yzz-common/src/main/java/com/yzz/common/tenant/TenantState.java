package com.yzz.common.tenant;

/**
 * 租户生命周期五态，与字典「租户状态」一一对应。
 */
public enum TenantState {

    TRIAL("试用中"),
    NORMAL("正常"),
    ARREARS("欠费"),
    DISABLED("停用"),
    EXPIRED("已到期"),
    UNKNOWN("未知");

    private final String label;

    TenantState(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static TenantState of(String code) {
        if (code == null || code.isBlank()) {
            return UNKNOWN;
        }
        for (TenantState state : values()) {
            if (state.name().equalsIgnoreCase(code.trim())) {
                return state;
            }
        }
        return UNKNOWN;
    }

    /** 是否允许访问（停用与到期一律拒绝）。 */
    public boolean accessible() {
        return this == TRIAL || this == NORMAL || this == ARREARS;
    }

    /** 是否限制写操作（欠费阶段按配置限制写操作）。 */
    public boolean writeLimited() {
        return this == ARREARS;
    }
}
