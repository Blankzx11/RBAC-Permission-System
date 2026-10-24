package com.rbac.audit.port;

import com.rbac.audit.domain.SecurityEvent;

public interface SecurityEventRecorder {
    /** 独立事务记录安全事件；返回false表示记录失败，不得因此放行原本被拒绝的请求。 */
    boolean recordSecurityEvent(SecurityEvent event);
}
