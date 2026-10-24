package com.example.rbac.rbac;

import com.example.rbac.common.Ids;
import com.example.rbac.security.SessionContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public AuditService(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    public void write(SessionContext context, String action, String targetType, String targetId,
                      Object before, Object after) {
        jdbc.update("""
                INSERT INTO iam_audit_log(id,sys_id,actor_id,actor_name,action,target_type,target_id,
                                          before_json,after_json,request_id,created_at)
                VALUES (?,?,?,?,?,?,?,?,?,?,CURRENT_TIMESTAMP)
                """, Ids.next(), context.sysId(), context.userId(), context.displayName(), action,
                targetType, targetId, json(before), json(after), context.requestId());
    }

    public List<RbacViews.AuditView> list(long sysId, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 200));
        return jdbc.query("""
                SELECT id, sys_id, actor_id, actor_name, action, target_type, target_id,
                       before_json, after_json, request_id, created_at
                  FROM iam_audit_log
                 WHERE sys_id = ?
                 ORDER BY created_at DESC, id DESC
                 LIMIT ?
                """, (rs, rowNum) -> new RbacViews.AuditView(
                        Long.toString(rs.getLong("id")),
                        rs.getObject("sys_id") == null ? null : Long.toString(rs.getLong("sys_id")),
                        rs.getObject("actor_id") == null ? null : Long.toString(rs.getLong("actor_id")),
                        rs.getString("actor_name"),
                        rs.getString("action"),
                        rs.getString("target_type"),
                        rs.getString("target_id"),
                        rs.getString("before_json"),
                        rs.getString("after_json"),
                        rs.getString("request_id"),
                        rs.getTimestamp("created_at").toInstant()), sysId, safeLimit);
    }

    private String json(Object value) {
        if (value == null) return null;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return "{\"serializationError\":true}";
        }
    }
}
