package com.rbac.audit.application;

import com.rbac.audit.domain.*;
import org.springframework.stereotype.Component;

@Component
public class AuditAccessPolicy {
    public void authorize(AuditActor actor, AuditScope scope) {
        if (scope instanceof AuditScope.Platform) {
            if (!actor.platformAdmin() || actor.sysId() != null)
                throw AuditFailure.forbidden("平台日志需要平台管理员的平台会话");
            return;
        }
        long requestedSystem = ((AuditScope.System) scope).sysId();
        if (actor.sysId() == null || actor.sysId() != requestedSystem)
            throw new AuditFailure(403, "SYSTEM_CONTEXT_MISMATCH", "路径系统与当前会话不一致");
        if (!actor.platformAdmin() && !actor.permissionCodes().contains("sys:audit:read"))
            throw AuditFailure.forbidden("缺少审计查询权限");
    }
}
