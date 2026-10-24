package com.rbac.audit.domain;

import java.util.Objects;
import java.util.Set;

/** 身份来自认证模块；请求参数不能直接构造可信身份。 */
public record AuditActor(long userId, String displayName, Long sysId, boolean platformAdmin,
                         Set<String> permissionCodes, String requestId, String ip) {
    public AuditActor {
        if (userId <= 0 || (sysId != null && sysId <= 0)) throw AuditFailure.invalid("身份ID无效");
        Objects.requireNonNull(displayName);
        Objects.requireNonNull(requestId);
        if (displayName.isBlank() || displayName.length() > 64) throw AuditFailure.invalid("操作者名称无效");
        if (!requestId.matches("[A-Za-z0-9._-]{1,80}")) throw AuditFailure.invalid("requestId无效");
        if (ip != null && !ip.matches("[0-9a-fA-F:.]{1,45}")) throw AuditFailure.invalid("来源IP无效");
        permissionCodes = Set.copyOf(permissionCodes);
    }
}
