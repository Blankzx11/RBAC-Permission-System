package com.rbac.audit.application;

import com.rbac.audit.domain.SecurityEvent;
import com.rbac.audit.functional.AuditFunctions;
import com.rbac.audit.infrastructure.SecurityEventJdbcRepository;
import com.rbac.audit.port.AuditIdGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;

@Service
public class SecurityEventStore {
    private final SecurityEventJdbcRepository repository;
    private final AuditIdGenerator ids;
    private final Clock clock;

    public SecurityEventStore(SecurityEventJdbcRepository repository, AuditIdGenerator ids, Clock clock) {
        this.repository = repository; this.ids = ids; this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void appendInNewTransaction(SecurityEvent event) {
        repository.insert(ids.nextId(), event, AuditFunctions.maskUsername(event.username()), clock.instant());
    }
}
