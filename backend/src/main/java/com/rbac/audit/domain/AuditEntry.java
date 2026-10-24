package com.rbac.audit.domain;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;

public record AuditEntry(long id, Long sysId, Long actorId, String actorName, String action,
                         String targetType, String targetId, JsonNode before, JsonNode after,
                         String requestId, String ip, Instant createdAt) { }
