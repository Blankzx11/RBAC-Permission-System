package com.rbac.audit.domain;

import java.util.Objects;

public record SecurityEvent(Long userId, String username, Long sysId, Reason reason,
                            String requestId, String ip) {
    public enum Reason {
        LOGIN_SUCCESS, BAD_CREDENTIALS, ACCOUNT_LOCKED, ACCOUNT_DISABLED,
        SESSION_EXPIRED, CONTEXT_MISMATCH, FORBIDDEN
    }

    public SecurityEvent {
        Objects.requireNonNull(reason);
        if (userId != null && userId <= 0 || sysId != null && sysId <= 0) throw AuditFailure.invalid("安全事件ID无效");
        if (requestId == null || !requestId.matches("[A-Za-z0-9._-]{1,80}")) throw AuditFailure.invalid("requestId无效");
        if (ip != null && !ip.matches("[0-9a-fA-F:.]{1,45}")) throw AuditFailure.invalid("来源IP无效");
        username = username == null ? "unknown" : username;
    }

    public String outcome() { return reason == Reason.LOGIN_SUCCESS ? "SUCCESS" : "DENIED"; }
}
