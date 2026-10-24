package com.rbac.audit.application;

import com.rbac.audit.domain.*;
import com.rbac.audit.functional.AuditFunctions;
import com.rbac.audit.port.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;

@Service
public class TransactionalAuditWriter implements AuditWriter {
    private final AuditRepository repository;
    private final AuditIdGenerator ids;
    private final Clock clock;

    public TransactionalAuditWriter(AuditRepository repository, AuditIdGenerator ids, Clock clock) {
        this.repository = repository; this.ids = ids; this.clock = clock;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public void appendInCurrentTransaction(AuditChange change) {
        AuditActor actor = change.actor();
        var before = AuditFunctions.redactSnapshot(change.targetType(), change.before());
        var after = AuditFunctions.redactSnapshot(change.targetType(), change.after());
        repository.insert(new AuditEntry(ids.nextId(), actor.sysId(), actor.userId(), actor.displayName(),
            change.action(), change.targetType().name(), change.targetId(), before, after,
            actor.requestId(), actor.ip(), clock.instant()));
    }
}
