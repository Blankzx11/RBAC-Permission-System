package com.rbac.audit.domain;

import java.time.Instant;
import java.util.List;

public final class AuditQuery {
    private AuditQuery() { }

    public record Input(Instant from, Instant to, Long actorId, String action, int page, int pageSize) { }

    public record Range(Instant from, Instant to) { }

    public record Criteria(AuditScope scope, Range range, Long actorId, String action, int page, int pageSize) {
        public long offset() { return ((long) page - 1) * pageSize; }
    }

    public record Page<T>(List<T> items, long total, int page, int pageSize) {
        public Page { items = List.copyOf(items); }
    }

    public record Difference(String field, ChangeType change, Object before, Object after) { }
    public enum ChangeType { ADDED, REMOVED, CHANGED }
}
