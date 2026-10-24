package com.rbac.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rbac.audit.domain.*;
import com.rbac.audit.infrastructure.SnowflakeAuditIdGenerator;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.HashSet;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class DomainAndIdTest {
    @Test void identityAndSnapshotsHaveDefensiveCopies() throws Exception {
        var permissions = new HashSet<>(Set.of("sys:audit:read"));
        var actor = new AuditActor(2,"actor",1L,false,permissions,"request-1","127.0.0.1");
        permissions.clear(); assertEquals(1, actor.permissionCodes().size());
        var original = new ObjectMapper().createObjectNode().put("version", "1");
        var change = new AuditChange(actor,"UPDATED_MEMBER",AuditChange.TargetType.MEMBER,"4",null,original);
        original.put("version", "2"); assertEquals("1", change.after().get("version").asText());
        ((com.fasterxml.jackson.databind.node.ObjectNode) change.after()).put("version", "3");
        assertEquals("1", change.after().get("version").asText());
    }

    @Test void idsAreUniqueWithinNodeAndSeparateAcrossNodes() {
        var clock = Clock.fixed(Instant.parse("2026-10-24T00:00:00Z"), ZoneOffset.UTC);
        var first = new SnowflakeAuditIdGenerator(clock, 0);
        var second = new SnowflakeAuditIdGenerator(clock, 1);
        Set<Long> values = new HashSet<>();
        for (int i = 0; i < 4096; i++) { assertTrue(values.add(first.nextId())); assertTrue(values.add(second.nextId())); }
        assertThrows(IllegalStateException.class, first::nextId);
        assertThrows(IllegalArgumentException.class, () -> new SnowflakeAuditIdGenerator(clock, 1024));
        assertThrows(IllegalStateException.class, () -> new SnowflakeAuditIdGenerator(Clock.fixed(Instant.EPOCH,ZoneOffset.UTC),0).nextId());
    }

    @Test void invalidDomainStatesAreRejected() {
        assertThrows(AuditFailure.class, () -> new AuditScope.System(0));
        assertThrows(AuditFailure.class, () -> new AuditScope.Platform(-1L));
        assertThrows(AuditFailure.class, () -> new AuditActor(0,"actor",null,true,Set.of(),"r",null));
        assertThrows(AuditFailure.class, () -> new SecurityEvent(1L,"user",1L,SecurityEvent.Reason.FORBIDDEN,"r\n",null));
        assertEquals("SUCCESS",new SecurityEvent(null,"user",null,SecurityEvent.Reason.LOGIN_SUCCESS,"r",null).outcome());
    }
}
