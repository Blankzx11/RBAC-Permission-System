package com.example.rbac.security;

import com.example.rbac.common.ApiException;
import com.example.rbac.common.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class SessionGate {
    public static final String SESSION_ATTRIBUTE = SessionContext.class.getName();
    private final JdbcTemplate jdbc;

    public SessionGate(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public SessionContext authenticate(HttpServletRequest request) {
        Object cached = request.getAttribute(SESSION_ATTRIBUTE);
        if (cached instanceof SessionContext context) {
            return context;
        }

        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            throw ApiException.unauthorized("缺少 Bearer 会话令牌");
        }
        String token = authorization.substring(7).trim();
        if (token.isBlank() || token.length() > 128) {
            throw ApiException.unauthorized("会话令牌格式不正确");
        }

        String sql = """
                SELECT s.session_id, s.user_id, s.sys_id, s.auth_version AS session_auth_version,
                       s.expires_at, s.revoked_at, u.username, u.display_name,
                       u.status AS user_status, u.platform_admin,
                       u.auth_version AS user_auth_version,
                       sy.status AS system_status, sy.policy_version,
                       m.status AS member_status
                  FROM iam_session s
                  JOIN iam_user u ON u.id = s.user_id
                  LEFT JOIN iam_system sy ON sy.id = s.sys_id
                  LEFT JOIN iam_member m ON m.sys_id = s.sys_id AND m.user_id = s.user_id
                 WHERE s.session_id = ?
                """;
        List<Map<String, Object>> rows = jdbc.queryForList(sql, token);
        if (rows.isEmpty()) {
            throw ApiException.unauthorized("会话不存在或已失效");
        }
        Map<String, Object> row = rows.getFirst();
        Timestamp expiresAt = (Timestamp) row.get("expires_at");
        if (row.get("revoked_at") != null || expiresAt == null || !expiresAt.toInstant().isAfter(Instant.now())) {
            throw ApiException.unauthorized("会话已过期或已撤销");
        }
        if (!"ENABLED".equals(row.get("user_status"))) {
            throw ApiException.unauthorized("账号已停用");
        }
        long sessionAuthVersion = ((Number) row.get("session_auth_version")).longValue();
        long userAuthVersion = ((Number) row.get("user_auth_version")).longValue();
        if (sessionAuthVersion != userAuthVersion) {
            throw ApiException.unauthorized("账号安全版本已变化，请重新登录");
        }

        Long sysId = row.get("sys_id") == null ? null : ((Number) row.get("sys_id")).longValue();
        boolean platformAdmin = Boolean.TRUE.equals(row.get("platform_admin"));
        long policyVersion = 0L;
        if (sysId != null) {
            if (!"ENABLED".equals(row.get("system_status"))) {
                throw ApiException.forbidden("当前子系统已停用");
            }
            policyVersion = ((Number) row.get("policy_version")).longValue();
            if (!platformAdmin && !"ENABLED".equals(row.get("member_status"))) {
                throw ApiException.forbidden("当前账号不是该系统的有效成员");
            }
        }

        SessionContext context = new SessionContext(
                token,
                ((Number) row.get("user_id")).longValue(),
                (String) row.get("username"),
                (String) row.get("display_name"),
                sysId,
                platformAdmin,
                policyVersion,
                RequestIdFilter.get(request));
        request.setAttribute(SESSION_ATTRIBUTE, context);
        return context;
    }

    public SessionContext require(HttpServletRequest request) {
        Object value = request.getAttribute(SESSION_ATTRIBUTE);
        if (value instanceof SessionContext context) {
            return context;
        }
        return authenticate(request);
    }
}
