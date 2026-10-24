package com.rbac.audit.port;

import java.util.Set;

/** A/E模块完成认证、会话及实时权限校验后，向Spring Security发布此类型的principal。 */
public interface AuditIdentity {
    long userId();
    String displayName();
    Long sysId();
    boolean platformAdmin();
    Set<String> permissionCodes();
}
