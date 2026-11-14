package com.yzz.common.audit;

/**
 * 审计落库入口。由 yzz-system 模块基于 JdbcTemplate 实现，公共层只依赖抽象。
 */
public interface AuditRecorder {

    void record(AuditEvent event);

    /** 安全审计：跨租户越权、登录失败等安全事件，必须留痕。 */
    void recordSecurity(String action, String detail, String ip);
}
