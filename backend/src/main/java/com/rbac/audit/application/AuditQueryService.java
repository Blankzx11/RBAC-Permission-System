package com.rbac.audit.application;

import com.rbac.audit.domain.*;
import com.rbac.audit.functional.AuditFunctions;
import com.rbac.audit.port.AuditRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;

/** 按鉴权、规范化、范围查询、结果投影的顺序组织用例。 */
@Service
public class AuditQueryService {
    private final AuditAccessPolicy access;
    private final AuditRepository repository;
    private final Clock clock;

    public AuditQueryService(AuditAccessPolicy access, AuditRepository repository, Clock clock) {
        this.access = access; this.repository = repository; this.clock = clock;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public AuditQuery.Page<AuditEntry> queryAudit(AuditActor actor, AuditScope scope, AuditQuery.Input input) {
        access.authorize(actor, scope);
        var criteria = AuditFunctions.normalizeQuery(scope, input, clock.instant());
        return repository.query(criteria);
    }

    @Transactional(readOnly = true)
    public AuditEntry getAuditDetail(AuditActor actor, AuditScope scope, long id) {
        access.authorize(actor, scope);
        return repository.findById(scope, id).orElseThrow(() -> new AuditFailure(404, "NOT_FOUND", "当前范围内日志不存在"));
    }
}
