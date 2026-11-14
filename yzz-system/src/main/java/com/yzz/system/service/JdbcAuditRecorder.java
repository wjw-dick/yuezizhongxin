package com.yzz.system.service;

import com.yzz.common.audit.AuditEvent;
import com.yzz.common.audit.AuditRecorder;
import com.yzz.common.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 审计落库实现：平台审计与租户审计物理分离。
 *
 * <p>写审计使用 JdbcTemplate 直写，避免被租户插件追加条件，也避免审计写入依赖业务事务。</p>
 */
@Service
public class JdbcAuditRecorder implements AuditRecorder {

    private static final Logger log = LoggerFactory.getLogger(JdbcAuditRecorder.class);

    private final JdbcTemplate jdbcTemplate;

    public JdbcAuditRecorder(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void record(AuditEvent event) {
        String result = event.success() ? "成功" : "失败";
        if (event.platform()) {
            jdbcTemplate.update("insert into platform_audit(module, action, target, detail, result, "
                            + "operator_id, operator_name, tenant_id, tenant_code, ip, created_time) "
                            + "values(?,?,?,?,?,?,?,?,?,?,?)",
                    event.module(), event.action(), event.target(), event.detail(), result,
                    event.operatorId(), event.operatorName(), event.tenantId(), event.tenantCode(),
                    event.ip(), LocalDateTime.now());
        } else {
            jdbcTemplate.update("insert into sys_audit_log(tenant_id, tenant_code, module, action, target, "
                            + "detail, result, operator_id, operator_name, ip, created_time) "
                            + "values(?,?,?,?,?,?,?,?,?,?,?)",
                    event.tenantId(), event.tenantCode(), event.module(), event.action(), event.target(),
                    event.detail(), result, event.operatorId(), event.operatorName(), event.ip(),
                    LocalDateTime.now());
        }
    }

    @Override
    public void recordSecurity(String action, String detail, String ip) {
        TenantContext.Holder holder = TenantContext.holder();
        jdbcTemplate.update("insert into sys_audit_log(tenant_id, tenant_code, module, action, target, detail, "
                        + "result, operator_id, operator_name, ip, created_time) "
                        + "values(?,?,?,?,?,?,?,?,?,?,?)",
                holder.getTenantId(), holder.getTenantCode(), "安全审计", action, "接口访问",
                detail, "失败", holder.getUserId(), holder.getUsername(), ip, LocalDateTime.now());
        log.warn("安全审计 action={} detail={} ip={}", action, detail, ip);
    }
}
