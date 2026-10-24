package com.rbac.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rbac.audit.port.AuditIdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"demo", "test"})
class AuditHttpTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    @MockitoBean Clock clock;
    @MockitoBean AuditIdGenerator ids;
    private static final String PASSWORD = "DemoOnly12345";

    @BeforeEach void seed() {
        when(clock.instant()).thenReturn(Instant.parse("2026-10-24T04:00:00Z"));
        var counter = new AtomicLong(1000); when(ids.nextId()).thenAnswer(call -> counter.incrementAndGet());
        jdbc.update("DELETE FROM iam_audit_log"); jdbc.update("DELETE FROM iam_login_log");
        row(11, 1L, 2L, "MEMBER_ROLES_REPLACED", "2026-10-24T01:00:00Z", "{\"roleIds\":[\"103\"],\"version\":1}");
        row(12, 2L, 3L, "UPDATED_MEMBER", "2026-10-24T01:00:00Z", "{\"status\":\"DISABLED\"}");
        row(13, null, 1L, "CREATED_SYSTEM", "2026-10-24T01:00:00Z", null);
        row(14, 1L, 2L, "UPDATED_MEMBER", "2026-10-24T02:00:00Z", "{\"displayName\":\"<script>alert(1)</script>\",\"password\":\"do-not-return\"}");
        row(Long.MAX_VALUE, 1L, 2L, "UPDATED_MEMBER", "2026-10-24T00:00:00Z", null);
    }

    private void row(long id, Long system, Long actor, String action, String at, String after) {
        jdbc.update("INSERT INTO iam_audit_log(id,sys_id,actor_id,actor_name,action,target_type,target_id,before_json,after_json,request_id,ip,created_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",
            id, system, actor, "历史操作者", action, "MEMBER", "4", null, after, "test-request-" + id, null, Timestamp.from(Instant.parse(at)));
    }

    @Test void allFourEndpointsReturnContractEnvelopes() throws Exception {
        mvc.perform(get("/api/v1/systems/1/audit-logs").with(httpBasic("auditor_a", PASSWORD))).andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(3)).andExpect(jsonPath("$.data.items[0].id").value("14"));
        mvc.perform(get("/api/v1/systems/1/audit-logs/11").with(httpBasic("auditor_a", PASSWORD))).andExpect(status().isOk()).andExpect(jsonPath("$.data.sysId").value("1"));
        mvc.perform(get("/api/v1/platform/audit-logs").with(httpBasic("platform_admin", PASSWORD))).andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(5));
        mvc.perform(get("/api/v1/platform/audit-logs/13").with(httpBasic("platform_admin", PASSWORD))).andExpect(status().isOk()).andExpect(jsonPath("$.data.sysId").isEmpty());
    }

    @Test void deniesAnonymousSpoofedAndUnprivilegedIdentities() throws Exception {
        mvc.perform(get("/api/v1/systems/1/audit-logs").header("X-User", "1").header("X-Platform-Admin", "true")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/systems/1/audit-logs").with(httpBasic("ordinary_a", PASSWORD)).header("X-Permissions", "sys:audit:read")).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/systems/1/audit-logs").with(user("untyped-user").roles("ADMIN"))).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/systems/1/audit-logs").with(httpBasic("auditor_a", "wrong"))).andExpect(status().isUnauthorized());
    }

    @Test void isolatesSystemPathsObjectsAndPlatformContext() throws Exception {
        mvc.perform(get("/api/v1/systems/2/audit-logs").with(httpBasic("auditor_a", PASSWORD))).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("SYSTEM_CONTEXT_MISMATCH"));
        mvc.perform(get("/api/v1/systems/1/audit-logs/12").with(httpBasic("auditor_a", PASSWORD))).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/platform/audit-logs").with(httpBasic("auditor_a", PASSWORD))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/platform/audit-logs").with(httpBasic("platform_a", PASSWORD))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/systems/1/audit-logs").with(httpBasic("platform_admin", PASSWORD))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/systems/1/audit-logs").with(httpBasic("platform_a", PASSWORD))).andExpect(status().isOk());
    }

    @Test void appliesFiltersPaginationAndExclusiveUpperBound() throws Exception {
        mvc.perform(get("/api/v1/platform/audit-logs").param("sysId", "2").with(httpBasic("platform_admin", PASSWORD))).andExpect(jsonPath("$.data.total").value(1));
        mvc.perform(get("/api/v1/platform/audit-logs").param("actorId", "3").with(httpBasic("platform_admin", PASSWORD))).andExpect(jsonPath("$.data.total").value(1));
        mvc.perform(get("/api/v1/systems/1/audit-logs").param("pageSize", "1").param("page", "2").with(httpBasic("auditor_a", PASSWORD))).andExpect(jsonPath("$.data.items[0].id").value("11"));
        mvc.perform(get("/api/v1/systems/1/audit-logs").param("from", "2026-10-24T01:00:00Z").param("to", "2026-10-24T02:00:00Z").with(httpBasic("auditor_a", PASSWORD))).andExpect(jsonPath("$.data.total").value(1));
        mvc.perform(get("/api/v1/systems/1/audit-logs").param("action", "' OR 1=1 --").with(httpBasic("auditor_a", PASSWORD))).andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(0));
    }

    @Test void rejectsInvalidAndAmbiguousQueryParameters() throws Exception {
        for (String invalid : new String[]{"0", "101", "-1", "text"}) mvc.perform(get("/api/v1/systems/1/audit-logs").param("pageSize", invalid).with(httpBasic("auditor_a", PASSWORD))).andExpect(status().isUnprocessableEntity());
        mvc.perform(get("/api/v1/systems/1/audit-logs").param("from", "2026-10-24T00:00:00Z").with(httpBasic("auditor_a", PASSWORD))).andExpect(status().isUnprocessableEntity());
        mvc.perform(get("/api/v1/systems/1/audit-logs").param("from", "2026-01-01T00:00:00Z").param("to", "2026-10-24T00:00:00Z").with(httpBasic("auditor_a", PASSWORD))).andExpect(status().isUnprocessableEntity());
        mvc.perform(get("/api/v1/systems/1/audit-logs").param("sysId", "2").with(httpBasic("auditor_a", PASSWORD))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/systems/1/audit-logs").param("page", "1", "2").with(httpBasic("auditor_a", PASSWORD))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/systems/1/audit-logs/9223372036854775808").with(httpBasic("auditor_a", PASSWORD))).andExpect(status().isUnprocessableEntity());
    }

    @Test void preservesStringIdsNullsTraceIdsAndRedactsLegacySnapshots() throws Exception {
        var result = mvc.perform(get("/api/v1/systems/1/audit-logs/9223372036854775807").with(httpBasic("auditor_a", PASSWORD)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value("9223372036854775807"))
            .andExpect(jsonPath("$.data.before").isEmpty()).andExpect(header().exists("X-Request-Id")).andReturn();
        var body = json.readTree(result.getResponse().getContentAsString());
        assertTrue(body.get("data").has("before")); assertTrue(body.get("data").has("after"));
        assertFalse(body.get("data").has("ip"));
        assertEquals(result.getResponse().getHeader("X-Request-Id"), body.get("requestId").asText());
        mvc.perform(get("/api/v1/systems/1/audit-logs/14").with(httpBasic("auditor_a", PASSWORD)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.after.password").doesNotExist());
    }

    @Test void noHttpAuditMutationEndpointExists() throws Exception {
        mvc.perform(post("/api/v1/platform/audit-logs").contentType("application/json").content("{}").with(httpBasic("platform_admin", PASSWORD))).andExpect(status().isMethodNotAllowed());
        mvc.perform(delete("/api/v1/platform/audit-logs/13").with(httpBasic("platform_admin", PASSWORD))).andExpect(status().isMethodNotAllowed());
    }

    @Test void auditDenialRemainsForbiddenWhenSecurityEventStorageFails() throws Exception {
        when(ids.nextId()).thenThrow(new IllegalStateException("unavailable"));
        mvc.perform(get("/api/v1/systems/1/audit-logs").with(httpBasic("ordinary_a", PASSWORD))).andExpect(status().isForbidden());
    }

    @Test void detailRejectsUnknownQueriesAndUnknownLegacySnapshotsStayHidden() throws Exception {
        mvc.perform(get("/api/v1/systems/1/audit-logs/11").param("sysId", "2").with(httpBasic("auditor_a", PASSWORD))).andExpect(status().isBadRequest());
        jdbc.update("UPDATE iam_audit_log SET target_type='UNKNOWN',after_json=? WHERE id=14", "{\"password\":\"hidden\"}");
        mvc.perform(get("/api/v1/systems/1/audit-logs/14").with(httpBasic("auditor_a", PASSWORD))).andExpect(status().isOk()).andExpect(jsonPath("$.data.after.redacted").value(true)).andExpect(jsonPath("$.data.after.password").doesNotExist());
    }
}
