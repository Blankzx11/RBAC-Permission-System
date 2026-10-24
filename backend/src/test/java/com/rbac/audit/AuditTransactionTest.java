package com.rbac.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rbac.audit.domain.*;
import com.rbac.audit.port.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles({"demo", "test"})
class AuditTransactionTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired TransactionTemplate tx;
    @Autowired AuditWriter writer;
    @Autowired SecurityEventRecorder events;
    @Autowired ObjectMapper json;
    @MockitoBean AuditIdGenerator ids;

    @BeforeEach void clean() {
        jdbc.execute("CREATE TABLE IF NOT EXISTS business_probe(id BIGINT PRIMARY KEY, value_text VARCHAR(40))");
        jdbc.update("DELETE FROM business_probe"); jdbc.update("DELETE FROM iam_audit_log"); jdbc.update("DELETE FROM iam_login_log");
        var sequence = new AtomicLong(1000);
        when(ids.nextId()).thenAnswer(call -> sequence.incrementAndGet());
    }

    private AuditChange change() throws Exception {
        var actor = new AuditActor(2, "审计操作者", 1L, false, Set.of("sys:audit:read"), "test-transaction", "127.0.0.1");
        return new AuditChange(actor, "MEMBER_ROLES_REPLACED", AuditChange.TargetType.MEMBER, "4", json.readTree("{\"roleIds\":[],\"version\":1}"), json.readTree("{\"roleIds\":[\"10\",\"2\",\"2\"],\"version\":2,\"password\":\"never-store\"}"));
    }

    private long count(String table) { return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Long.class); }
    private SecurityEvent event() { return new SecurityEvent(2L, "alice", 1L, SecurityEvent.Reason.FORBIDDEN, "test-event", "127.0.0.1"); }

    @Test void mandatoryRejectsCallsWithoutBusinessTransaction() throws Exception {
        var change = change();
        assertThrows(IllegalTransactionStateException.class, () -> writer.appendInCurrentTransaction(change));
        assertEquals(0, count("iam_audit_log"));
    }

    @Test void commitsBusinessAndAuditTogetherWithCanonicalSnapshots() throws Exception {
        var change = change();
        tx.executeWithoutResult(status -> { jdbc.update("INSERT INTO business_probe VALUES(1,'changed')"); writer.appendInCurrentTransaction(change); });
        assertEquals(1, count("business_probe")); assertEquals(1, count("iam_audit_log"));
        String after = jdbc.queryForObject("SELECT after_json FROM iam_audit_log", String.class);
        assertEquals("{\"roleIds\":[\"2\",\"10\"],\"version\":\"2\"}", after);
        assertFalse(after.contains("never-store"));
    }

    @Test void businessFailureRollsBackAlreadyInsertedAudit() throws Exception {
        var change = change();
        assertThrows(IllegalStateException.class, () -> tx.executeWithoutResult(status -> {
            jdbc.update("INSERT INTO business_probe VALUES(1,'changed')"); writer.appendInCurrentTransaction(change); throw new IllegalStateException("business failure");
        }));
        assertEquals(0, count("business_probe")); assertEquals(0, count("iam_audit_log"));
    }

    @Test void auditInsertFailureRollsBackBusinessAndCannotBeSilentlyCaught() throws Exception {
        var change = change();
        when(ids.nextId()).thenReturn(99L);
        tx.executeWithoutResult(status -> writer.appendInCurrentTransaction(change));
        assertThrows(DataAccessException.class, () -> tx.executeWithoutResult(status -> {
            jdbc.update("INSERT INTO business_probe VALUES(1,'changed')"); writer.appendInCurrentTransaction(change);
        }));
        assertEquals(0, count("business_probe")); assertEquals(1, count("iam_audit_log"));
        assertThrows(UnexpectedRollbackException.class, () -> tx.executeWithoutResult(status -> {
            jdbc.update("INSERT INTO business_probe VALUES(1,'changed')");
            try { writer.appendInCurrentTransaction(change); } catch (DataAccessException ignored) { }
        }));
        assertEquals(0, count("business_probe"));
    }

    @Test void failureSecurityEventSurvivesOuterRollback() {
        assertThrows(IllegalStateException.class, () -> tx.executeWithoutResult(status -> {
            jdbc.update("INSERT INTO business_probe VALUES(1,'changed')"); assertTrue(events.recordSecurityEvent(event()));
            throw new IllegalStateException("outer rollback");
        }));
        assertEquals(0, count("business_probe")); assertEquals(1, count("iam_login_log"));
        assertEquals("a***e", jdbc.queryForObject("SELECT username FROM iam_login_log", String.class));
        assertEquals("DENIED", jdbc.queryForObject("SELECT outcome FROM iam_login_log", String.class));
    }

    @Test void securityLogFailureDoesNotRollBackBusinessOrTurnDenialIntoSuccess() {
        when(ids.nextId()).thenThrow(new IllegalStateException("storage unavailable"));
        tx.executeWithoutResult(status -> { jdbc.update("INSERT INTO business_probe VALUES(1,'changed')"); assertFalse(events.recordSecurityEvent(event())); });
        assertEquals(1, count("business_probe")); assertEquals(0, count("iam_login_log"));
    }
}
