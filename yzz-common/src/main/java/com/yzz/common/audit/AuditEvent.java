package com.yzz.common.audit;

import java.time.LocalDateTime;

/**
 * 审计事件。平台审计与租户审计分离落库：平台上下文写 platform_audit，租户上下文写 sys_audit_log。
 */
public record AuditEvent(
        String module,
        String action,
        String target,
        String detail,
        boolean success,
        String errorMsg,
        Long tenantId,
        String tenantCode,
        boolean platform,
        Long operatorId,
        String operatorName,
        String ip,
        LocalDateTime occurredAt) {
}
