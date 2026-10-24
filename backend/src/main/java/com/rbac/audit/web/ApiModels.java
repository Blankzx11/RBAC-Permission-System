package com.rbac.audit.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.rbac.audit.domain.*;
import com.rbac.audit.functional.AuditFunctions;
import java.time.Instant;
import java.util.Map;

public final class ApiModels {
    private ApiModels() { }
    public record Success<T>(T data, String requestId) { }
    public record Error(String code, String message, String requestId, Map<String, Object> details) { }
    public record AuditView(String id, String sysId, String actorId, String actorName, String action,
                            String targetType, String targetId, JsonNode before, JsonNode after,
                            String requestId, Instant createdAt, @JsonInclude(JsonInclude.Include.NON_NULL) String ip) {
        public static AuditView from(AuditEntry e) {
            return new AuditView(Long.toString(e.id()), nullableId(e.sysId()), nullableId(e.actorId()), e.actorName(), e.action(),
                e.targetType(), e.targetId(), safeSnapshot(e.targetType(), e.before()), safeSnapshot(e.targetType(), e.after()), e.requestId(), e.createdAt(), e.ip());
        }
    }
    private static String nullableId(Long value) { return value == null ? null : value.toString(); }

    private static JsonNode safeSnapshot(String type, JsonNode value) {
        if (value == null || value.isNull()) return null;
        try { return AuditFunctions.redactSnapshot(AuditChange.TargetType.valueOf(type), value); }
        catch (IllegalArgumentException | AuditFailure ex) {
            // 遗留快照字段不可信时隐藏内容，不把潜在凭据返回页面。
            return com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode().put("redacted", true);
        }
    }
}
