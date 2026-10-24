package com.rbac.audit.demo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rbac.audit.domain.*;
import com.rbac.audit.port.AuditWriter;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.Set;
import java.util.UUID;

@Component
@Profile("demo & !test")
public class DemoDataInitializer implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    private final AuditWriter writer;
    private final ObjectMapper mapper;

    public DemoDataInitializer(JdbcTemplate jdbc, TransactionTemplate transactions, AuditWriter writer, ObjectMapper mapper) {
        this.jdbc = jdbc; this.transactions = transactions; this.writer = writer; this.mapper = mapper;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (jdbc.queryForObject("SELECT COUNT(*) FROM iam_audit_log", Long.class) > 0) return;
        var platform = actor(1, "平台管理员", null, true);
        var systemA = actor(2, "系统A管理员", 1L, false);
        var systemB = actor(3, "系统B管理员", 2L, false);
        var changes = java.util.List.of(
            new AuditChange(platform, "CREATED_SYSTEM", AuditChange.TargetType.SYSTEM, "1", null, mapper.readTree("{\"id\":\"1\",\"code\":\"EDU\",\"name\":\"教务系统\",\"status\":\"ENABLED\"}")),
            new AuditChange(systemA, "MEMBER_ROLES_REPLACED", AuditChange.TargetType.MEMBER, "4", mapper.readTree("{\"roleIds\":[\"103\"],\"version\":\"1\"}"), mapper.readTree("{\"roleIds\":[\"103\",\"102\"],\"version\":\"2\"}")),
            new AuditChange(systemA, "ROLE_PERMISSIONS_REPLACED", AuditChange.TargetType.ROLE, "102", mapper.readTree("{\"permissionIds\":[],\"version\":\"1\"}"), mapper.readTree("{\"permissionIds\":[\"1004\",\"1023\"],\"version\":\"2\"}")),
            new AuditChange(systemB, "UPDATED_MEMBER", AuditChange.TargetType.MEMBER, "4", mapper.readTree("{\"status\":\"ENABLED\"}"), mapper.readTree("{\"status\":\"DISABLED\"}"))
        );
        transactions.executeWithoutResult(status -> changes.forEach(writer::appendInCurrentTransaction));
    }

    private AuditActor actor(long id, String name, Long systemId, boolean platform) {
        return new AuditActor(id, name, systemId, platform, Set.of("sys:audit:read"), UUID.randomUUID().toString(), "127.0.0.1");
    }
}
