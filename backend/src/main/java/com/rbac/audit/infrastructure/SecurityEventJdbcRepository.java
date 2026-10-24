package com.rbac.audit.infrastructure;

import com.rbac.audit.domain.SecurityEvent;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;

@Repository
public class SecurityEventJdbcRepository {
    private final NamedParameterJdbcTemplate jdbc;
    public SecurityEventJdbcRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    public void insert(long id, SecurityEvent event, String maskedUsername, Instant at) {
        var params = new MapSqlParameterSource().addValue("id", id).addValue("userId", event.userId(), Types.BIGINT)
            .addValue("username", maskedUsername).addValue("sysId", event.sysId(), Types.BIGINT)
            .addValue("outcome", event.outcome()).addValue("reason", event.reason().name())
            .addValue("ip", event.ip(), Types.VARCHAR).addValue("requestId", event.requestId()).addValue("at", Timestamp.from(at));
        jdbc.update("INSERT INTO iam_login_log(id,user_id,username,sys_id,outcome,reason,ip,request_id,created_at) VALUES(:id,:userId,:username,:sysId,:outcome,:reason,:ip,:requestId,:at)", params);
    }
}
