package com.example.rbac.security;

import com.example.rbac.common.ApiException;

public record SessionContext(
        String sessionId,
        long userId,
        String username,
        String displayName,
        Long sysId,
        boolean platformAdmin,
        long policyVersion,
        String requestId) {

    public String sysIdString() {
        return sysId == null ? null : Long.toString(sysId);
    }

    public long requireSystemId() {
        if (sysId == null) {
            throw ApiException.badRequest("SYSTEM_CONTEXT_REQUIRED", "当前是平台上下文，请先切换到业务系统");
        }
        return sysId;
    }

    public boolean hasSystemContext() {
        return sysId != null;
    }
}
