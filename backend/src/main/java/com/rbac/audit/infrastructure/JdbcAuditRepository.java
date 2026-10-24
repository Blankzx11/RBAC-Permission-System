package com.rbac.audit.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rbac.audit.domain.*;
import com.rbac.audit.port.AuditRepository;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Optional;

@Repository
public class JdbcAuditRepository implements AuditRepository {
    private static final String COLUMNS = "id,sys_id,actor_id,actor_name,action,target_type,target_id,before_json,after_json,request_id,ip,created_at";
    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final RowMapper<AuditEntry> rowMapper;

    public JdbcAuditRepository(NamedParameterJdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc; this.mapper = mapper;
        this.rowMapper = (rs, row) -> new AuditEntry(rs.getLong("id"), rs.getObject("sys_id", Long.class),
            rs.getObject("actor_id", Long.class), rs.getString("actor_name"), rs.getString("action"),
            rs.getString("target_type"), rs.getString("target_id"), parse(rs.getString("before_json")),
            parse(rs.getString("after_json")), rs.getString("request_id"), rs.getString("ip"), rs.getTimestamp("created_at").toInstant());
    }

    @Override
    public void insert(AuditEntry e) {
        var params = new MapSqlParameterSource().addValue("id", e.id()).addValue("sysId", e.sysId(), Types.BIGINT)
            .addValue("actorId", e.actorId(), Types.BIGINT).addValue("actorName", e.actorName())
            .addValue("action", e.action()).addValue("targetType", e.targetType()).addValue("targetId", e.targetId())
            .addValue("before", stringify(e.before()), Types.VARCHAR).addValue("after", stringify(e.after()), Types.VARCHAR)
            .addValue("requestId", e.requestId()).addValue("ip", e.ip(), Types.VARCHAR).addValue("at", Timestamp.from(e.createdAt()));
        jdbc.update("INSERT INTO iam_audit_log (" + COLUMNS + ") VALUES (:id,:sysId,:actorId,:actorName,:action,:targetType,:targetId,:before,:after,:requestId,:ip,:at)", params);
    }

    @Override
    public AuditQuery.Page<AuditEntry> query(AuditQuery.Criteria criteria) {
        var params = new MapSqlParameterSource();
        String where = scopeSql(criteria.scope(), params) + " AND created_at >= :from AND created_at < :to";
        params.addValue("from", Timestamp.from(criteria.range().from())).addValue("to", Timestamp.from(criteria.range().to()));
        if (criteria.actorId() != null) { where += " AND actor_id = :actorId"; params.addValue("actorId", criteria.actorId()); }
        if (criteria.action() != null) { where += " AND action = :action"; params.addValue("action", criteria.action()); }
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM iam_audit_log WHERE " + where, params, Long.class);
        params.addValue("size", criteria.pageSize()).addValue("offset", criteria.offset());
        var items = jdbc.query("SELECT " + COLUMNS + " FROM iam_audit_log WHERE " + where + " ORDER BY created_at DESC,id DESC LIMIT :size OFFSET :offset", params, rowMapper);
        return new AuditQuery.Page<>(items, total == null ? 0 : total, criteria.page(), criteria.pageSize());
    }

    @Override
    public Optional<AuditEntry> findById(AuditScope scope, long id) {
        var params = new MapSqlParameterSource("id", id);
        String where = scopeSql(scope, params) + " AND id = :id";
        return jdbc.query("SELECT " + COLUMNS + " FROM iam_audit_log WHERE " + where, params, rowMapper).stream().findFirst();
    }

    private String scopeSql(AuditScope scope, MapSqlParameterSource params) {
        if (scope instanceof AuditScope.System system) { params.addValue("scopeSysId", system.sysId()); return "sys_id = :scopeSysId"; }
        if (scope instanceof AuditScope.Platform platform) {
            if (platform.filterSysId() != null) { params.addValue("scopeSysId", platform.filterSysId()); return "sys_id = :scopeSysId"; }
            return "1 = 1"; // 只有经过平台会话鉴权的显式Platform范围可以查询全平台。
        }
        throw new IllegalArgumentException("Audit scope is required");
    }

    private JsonNode parse(String json) {
        if (json == null) return null;
        try { return mapper.readTree(json); }
        catch (JsonProcessingException ex) { throw new DataRetrievalFailureException("Invalid audit snapshot"); }
    }

    private String stringify(JsonNode json) { return json == null ? null : json.toString(); }
}
