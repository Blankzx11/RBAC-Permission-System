package com.rbac.audit.web;

import com.rbac.audit.domain.AuditActor;
import com.rbac.audit.domain.AuditFailure;
import com.rbac.audit.port.AuditIdentity;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentAuditActor {
    public AuditActor current(HttpServletRequest request) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof AuditIdentity identity))
            throw new AuditFailure(401, "UNAUTHENTICATED", "需要认证模块提供有效的审计身份");
        // 不从X-User、X-SysId或X-Forwarded-For等客户端头读取身份和来源事实。
        return new AuditActor(identity.userId(), identity.displayName(), identity.sysId(), identity.platformAdmin(),
            identity.permissionCodes(), RequestContextFilter.requestId(request), request.getRemoteAddr());
    }
}
