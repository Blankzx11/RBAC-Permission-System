package com.rbac.audit.domain;

/** 平台查询和系统查询使用不同类型，避免null被意外解释为不加范围条件。 */
public sealed interface AuditScope permits AuditScope.Platform, AuditScope.System {
    record Platform(Long filterSysId) implements AuditScope {
        public Platform {
            if (filterSysId != null && filterSysId <= 0) throw AuditFailure.invalid("系统ID无效");
        }
    }
    record System(long sysId) implements AuditScope {
        public System {
            if (sysId <= 0) throw AuditFailure.invalid("系统ID无效");
        }
    }
}
