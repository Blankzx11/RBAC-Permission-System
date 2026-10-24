package com.rbac.audit.domain;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Objects;

public record AuditChange(AuditActor actor, String action, TargetType targetType,
                          String targetId, JsonNode before, JsonNode after) {
    public enum TargetType { USER, SYSTEM, MEMBER, ROLE, PERMISSION }

    public AuditChange {
        Objects.requireNonNull(actor);
        Objects.requireNonNull(targetType);
        if (action == null || !action.matches("[A-Z][A-Z0-9_]{0,79}")) throw AuditFailure.invalid("审计操作类型无效");
        if (targetId == null || !targetId.matches("[A-Za-z0-9:_-]{1,80}")) throw AuditFailure.invalid("审计目标标识无效");
        before = before == null ? null : before.deepCopy();
        after = after == null ? null : after.deepCopy();
    }

    @Override public JsonNode before() { return before == null ? null : before.deepCopy(); }
    @Override public JsonNode after() { return after == null ? null : after.deepCopy(); }
}
