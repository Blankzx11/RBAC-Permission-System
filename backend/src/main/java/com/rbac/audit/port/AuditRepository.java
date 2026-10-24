package com.rbac.audit.port;

import com.rbac.audit.domain.AuditEntry;
import com.rbac.audit.domain.AuditQuery;
import com.rbac.audit.domain.AuditScope;
import java.util.Optional;

public interface AuditRepository {
    void insert(AuditEntry entry);
    AuditQuery.Page<AuditEntry> query(AuditQuery.Criteria criteria);
    Optional<AuditEntry> findById(AuditScope scope, long id);
}
