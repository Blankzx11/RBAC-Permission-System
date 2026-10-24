package com.rbac.audit.port;

import com.rbac.audit.domain.AuditChange;

public interface AuditWriter {
    /** 必须在调用方现有事务内使用，失败时调用方业务一同回滚。 */
    void appendInCurrentTransaction(AuditChange change);
}
