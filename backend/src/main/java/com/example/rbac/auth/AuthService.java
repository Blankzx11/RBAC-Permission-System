package com.example.rbac.auth;

import com.example.rbac.common.ApiException;
import com.example.rbac.common.Ids;
import com.example.rbac.common.PasswordHasher;
import com.example.rbac.common.RequestIdFilter;
import com.example.rbac.rbac.EffectivePermissions;
import com.example.rbac.rbac.PermissionService;
import com.example.rbac.security.SessionContext;
import jakarta.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private record UserRow(long id, String username, String passwordHash, String displayName,
                           String status, boolean platformAdmin, long authVersion, Timestamp lockedUntil) {
    }

    private final JdbcTemplate jdbc;
    private final PermissionService permissionService;
    private final long sessionMinutes;

    public AuthService(JdbcTemplate jdbc, PermissionService permissionService,
                       @Value("${rbac.session-minutes:30}") long sessionMinutes) {
        this.jdbc = jdbc;
        this.permissionService = permissionService;
        this.sessionMinutes = sessionMinutes;
    }

    @Transactional
    public TokenView login(LoginRequest request, HttpServletRequest httpRequest) {
        String username = request.username().trim().toLowerCase();
        List<UserRow> users = jdbc.query("""
                SELECT id, username, password_hash, display_name, status, platform_admin,
                       auth_version, locked_until
                  FROM iam_user WHERE username = ?
                """, (rs, rowNum) -> new UserRow(
                        rs.getLong("id"),
                        rs.getString("username"),
                        rs.getString("password_hash"),
                        rs.getString("display_name"),
                        rs.getString("status"),
                        rs.getBoolean("platform_admin"),
                        rs.getLong("auth_version"),
                        rs.getTimestamp("locked_until")), username);
        if (users.isEmpty()) {
            recordLogin(null, username, parseNullableId(request.sysId()), "DENIED", "BAD_CREDENTIALS", httpRequest);
            throw ApiException.unauthorized("用户名或密码错误");
        }
        UserRow user = users.getFirst();
        if (!"ENABLED".equals(user.status())
                || (user.lockedUntil() != null && user.lockedUntil().toInstant().isAfter(Instant.now()))
                || !PasswordHasher.matches(request.password(), user.passwordHash())) {
            recordLogin(user.id(), username, parseNullableId(request.sysId()), "DENIED", "BAD_CREDENTIALS", httpRequest);
            throw ApiException.unauthorized("用户名或密码错误");
        }
        Long sysId = parseNullableId(request.sysId());
        validateSystemAccess(user.id(), user.platformAdmin(), sysId);
        recordLogin(user.id(), username, sysId, "SUCCESS", "OK", httpRequest);
        return createSession(user, sysId);
    }

    @Transactional
    public TokenView switchContext(SessionContext current, ContextRequest request, HttpServletRequest httpRequest) {
        long targetSysId = parseRequiredId(request.sysId());
        List<UserRow> users = jdbc.query("""
                SELECT id, username, password_hash, display_name, status, platform_admin,
                       auth_version, locked_until
                  FROM iam_user WHERE id = ?
                """, (rs, rowNum) -> new UserRow(
                        rs.getLong("id"), rs.getString("username"), rs.getString("password_hash"),
                        rs.getString("display_name"), rs.getString("status"), rs.getBoolean("platform_admin"),
                        rs.getLong("auth_version"), rs.getTimestamp("locked_until")), current.userId());
        if (users.isEmpty()) throw ApiException.unauthorized("账号不存在");
        UserRow user = users.getFirst();
        validateSystemAccess(user.id(), user.platformAdmin(), targetSysId);
        jdbc.update("UPDATE iam_session SET revoked_at = CURRENT_TIMESTAMP WHERE session_id = ?",
                current.sessionId());
        recordLogin(user.id(), user.username(), targetSysId, "SUCCESS", "CONTEXT_SWITCH", httpRequest);
        return createSession(user, targetSysId);
    }

    public void logout(SessionContext context) {
        jdbc.update("UPDATE iam_session SET revoked_at = CURRENT_TIMESTAMP WHERE session_id = ?",
                context.sessionId());
    }

    public MeView me(SessionContext context) {
        if (context.sysId() == null) {
            return new MeView(Long.toString(context.userId()), context.username(), context.displayName(),
                    null, context.platformAdmin(), List.of(), null);
        }
        EffectivePermissions effective = permissionService.resolve(context);
        return new MeView(Long.toString(context.userId()), context.username(), context.displayName(),
                Long.toString(context.sysId()), context.platformAdmin(),
                effective.directRoleIds(), effective.policyVersion());
    }

    public List<SystemView> systems(SessionContext context) {
        String sql;
        Object[] args;
        if (context.platformAdmin()) {
            sql = """
                    SELECT id, code, name, description, status, policy_version, version, created_at
                      FROM iam_system WHERE status = 'ENABLED' ORDER BY id
                    """;
            args = new Object[0];
        } else {
            sql = """
                    SELECT s.id, s.code, s.name, s.description, s.status,
                           s.policy_version, s.version, s.created_at
                      FROM iam_system s
                      JOIN iam_member m ON m.sys_id = s.id AND m.user_id = ?
                     WHERE s.status = 'ENABLED' AND m.status = 'ENABLED'
                     ORDER BY s.id
                    """;
            args = new Object[]{context.userId()};
        }
        return jdbc.query(sql, (rs, rowNum) -> new SystemView(
                Long.toString(rs.getLong("id")),
                rs.getString("code"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getString("status"),
                Long.toString(rs.getLong("policy_version")),
                Long.toString(rs.getLong("version")),
                rs.getTimestamp("created_at").toInstant()), args);
    }

    public String requestId(HttpServletRequest request) {
        return RequestIdFilter.get(request);
    }

    private void validateSystemAccess(long userId, boolean platformAdmin, Long sysId) {
        if (sysId == null) return;
        List<String> systems = jdbc.query(
                "SELECT status FROM iam_system WHERE id = ?",
                (rs, rowNum) -> rs.getString(1), sysId);
        if (systems.isEmpty() || !"ENABLED".equals(systems.getFirst())) {
            throw ApiException.forbidden("目标子系统不存在或已停用");
        }
        if (!platformAdmin) {
            List<String> members = jdbc.query("""
                    SELECT status FROM iam_member WHERE sys_id = ? AND user_id = ?
                    """, (rs, rowNum) -> rs.getString(1), sysId, userId);
            if (members.isEmpty() || !"ENABLED".equals(members.getFirst())) {
                throw ApiException.forbidden("账号不是目标子系统的有效成员");
            }
        }
    }

    private TokenView createSession(UserRow user, Long sysId) {
        String token = UUID.randomUUID().toString();
        Instant now = Instant.now();
        Instant expiresAt = now.plus(Duration.ofMinutes(sessionMinutes));
        jdbc.update("""
                INSERT INTO iam_session(session_id,user_id,sys_id,auth_version,expires_at,created_at)
                VALUES (?,?,?,?,?,?)
                """, token, user.id(), sysId, user.authVersion(), Timestamp.from(expiresAt), Timestamp.from(now));
        return new TokenView(token, "Bearer", sessionMinutes * 60L, token,
                sysId == null ? null : Long.toString(sysId));
    }

    private void recordLogin(Long userId, String username, Long sysId, String outcome,
                             String reason, HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        jdbc.update("""
                INSERT INTO iam_login_log(id,user_id,username,sys_id,outcome,reason,ip,request_id,created_at)
                VALUES (?,?,?,?,?,?,?,?,CURRENT_TIMESTAMP)
                """, Ids.next(), userId, username, sysId, outcome, reason, ip, RequestIdFilter.get(request));
    }

    private static Long parseNullableId(String value) {
        if (value == null || value.isBlank()) return null;
        return parseRequiredId(value);
    }

    private static long parseRequiredId(String value) {
        try {
            long parsed = Long.parseLong(value);
            if (parsed <= 0) throw new NumberFormatException();
            return parsed;
        } catch (NumberFormatException ex) {
            throw ApiException.badRequest("INVALID_SYS_ID", "sysId 必须是正整数");
        }
    }
}
