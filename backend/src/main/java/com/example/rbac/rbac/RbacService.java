package com.example.rbac.rbac;

import com.example.rbac.common.ApiException;
import com.example.rbac.common.Ids;
import com.example.rbac.security.SessionContext;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RbacService {
    private static final Set<String> ROUTES = Set.of(
            "/overview", "/members", "/roles", "/permissions", "/audit", "/profile");

    private final JdbcTemplate jdbc;
    private final AuditService auditService;

    public RbacService(JdbcTemplate jdbc, AuditService auditService) {
        this.jdbc = jdbc;
        this.auditService = auditService;
    }

    public List<RbacViews.RoleView> roles(long sysId) {
        return jdbc.query("""
                SELECT id, sys_id, code, name, description, status, builtin, version, created_at
                  FROM iam_role WHERE sys_id = ? ORDER BY code
                """, (rs, rowNum) -> roleView(rs), sysId);
    }

    public RbacViews.RoleView role(long sysId, long roleId) {
        List<RbacViews.RoleView> rows = jdbc.query("""
                SELECT id, sys_id, code, name, description, status, builtin, version, created_at
                  FROM iam_role WHERE sys_id = ? AND id = ?
                """, (rs, rowNum) -> roleView(rs), sysId, roleId);
        if (rows.isEmpty()) throw ApiException.notFound("角色不存在");
        return rows.getFirst();
    }

    @Transactional
    public RbacViews.RoleView createRole(SessionContext context, RbacRequests.RoleCreate request) {
        long sysId = context.requireSystemId();
        Integer duplicates = jdbc.queryForObject(
                "SELECT COUNT(*) FROM iam_role WHERE sys_id = ? AND code = ?",
                Integer.class, sysId, request.code());
        if (duplicates != null && duplicates > 0) {
            throw ApiException.conflict("ROLE_CODE_EXISTS", "角色编码在系统内已存在");
        }
        long id = Ids.next();
        String description = request.description() == null ? "" : request.description();
        jdbc.update("""
                INSERT INTO iam_role(id,sys_id,code,name,description,builtin,status,version,created_by)
                VALUES (?,?,?,?,?,FALSE,'ENABLED',1,?)
                """, id, sysId, request.code(), request.name(), description, context.userId());
        jdbc.update("UPDATE iam_system SET policy_version = policy_version + 1 WHERE id = ?", sysId);
        auditService.write(context, "ROLE_CREATED", "ROLE", Long.toString(id), null,
                java.util.Map.of("code", request.code(), "name", request.name()));
        return role(sysId, id);
    }

    @Transactional
    public RbacViews.RoleView updateRole(SessionContext context, long sysId, long roleId,
                                         RbacRequests.RolePatch request) {
        RbacViews.RoleView current = role(sysId, roleId);
        if (current.builtin()) {
            throw ApiException.conflict("BUILTIN_ROLE_PROTECTED", "内置角色不可修改");
        }
        if (request.name() == null && request.description() == null && request.status() == null) {
            throw ApiException.badRequest("EMPTY_PATCH", "至少提供一个需要修改的字段");
        }
        String name = request.name() == null ? current.name() : request.name();
        String description = request.description() == null ? current.description() : request.description();
        String status = request.status() == null ? current.status() : request.status();
        jdbc.update("""
                UPDATE iam_role SET name = ?, description = ?, status = ?, version = version + 1,
                                    updated_at = CURRENT_TIMESTAMP
                 WHERE sys_id = ? AND id = ?
                """, name, description, status, sysId, roleId);
        jdbc.update("UPDATE iam_system SET policy_version = policy_version + 1 WHERE id = ?", sysId);
        auditService.write(context, "ROLE_UPDATED", "ROLE", Long.toString(roleId), current, role(sysId, roleId));
        return role(sysId, roleId);
    }

    @Transactional
    public void deleteRole(SessionContext context, long sysId, long roleId) {
        RbacViews.RoleView current = role(sysId, roleId);
        if (current.builtin()) {
            throw ApiException.conflict("BUILTIN_ROLE_PROTECTED", "内置角色不可删除");
        }
        Integer assigned = jdbc.queryForObject(
                "SELECT COUNT(*) FROM iam_user_role WHERE sys_id = ? AND role_id = ?",
                Integer.class, sysId, roleId);
        if (assigned != null && assigned > 0) {
            throw ApiException.conflict("ROLE_IN_USE", "角色仍被成员引用，不能删除");
        }
        jdbc.update("DELETE FROM iam_role_permission WHERE sys_id = ? AND role_id = ?", sysId, roleId);
        jdbc.update("DELETE FROM iam_role WHERE sys_id = ? AND id = ?", sysId, roleId);
        jdbc.update("UPDATE iam_system SET policy_version = policy_version + 1 WHERE id = ?", sysId);
        auditService.write(context, "ROLE_DELETED", "ROLE", Long.toString(roleId), current, null);
    }

    public List<RbacViews.PermissionView> permissions(long sysId) {
        return jdbc.query("""
                SELECT id, sys_id, code, name, type, parent_id, route_path, sort_order,
                       status, version, created_at
                  FROM iam_permission WHERE sys_id = ?
                 ORDER BY sort_order, id
                """, (rs, rowNum) -> permissionView(rs), sysId);
    }

    public RbacViews.PermissionView permission(long sysId, long permissionId) {
        List<RbacViews.PermissionView> rows = jdbc.query("""
                SELECT id, sys_id, code, name, type, parent_id, route_path, sort_order,
                       status, version, created_at
                  FROM iam_permission WHERE sys_id = ? AND id = ?
                """, (rs, rowNum) -> permissionView(rs), sysId, permissionId);
        if (rows.isEmpty()) throw ApiException.notFound("权限不存在");
        return rows.getFirst();
    }

    @Transactional
    public RbacViews.PermissionView createPermission(SessionContext context,
                                                     RbacRequests.PermissionCreate request) {
        long sysId = context.requireSystemId();
        Long parentId = parseNullable(request.parentId());
        validatePermissionShape(sysId, request.type(), request.routePath(), parentId, null);
        Integer duplicates = jdbc.queryForObject(
                "SELECT COUNT(*) FROM iam_permission WHERE sys_id = ? AND code = ?",
                Integer.class, sysId, request.code());
        if (duplicates != null && duplicates > 0) {
            throw ApiException.conflict("PERMISSION_CODE_EXISTS", "权限编码在系统内已存在");
        }
        long id = Ids.next();
        int sortOrder = request.sortOrder() == null ? 0 : request.sortOrder();
        jdbc.update("""
                INSERT INTO iam_permission(id,sys_id,code,name,type,parent_id,route_path,
                                           sort_order,status,version,created_by)
                VALUES (?,?,?,?,?,?,?,?,'ENABLED',1,?)
                """, id, sysId, request.code(), request.name(), request.type(), parentId,
                request.routePath(), sortOrder, context.userId());
        bumpPolicy(sysId);
        auditService.write(context, "PERMISSION_CREATED", "PERMISSION", Long.toString(id), null,
                java.util.Map.of("code", request.code(), "type", request.type()));
        return permission(sysId, id);
    }

    @Transactional
    public RbacViews.PermissionView updatePermission(SessionContext context, long sysId, long permissionId,
                                                     RbacRequests.PermissionPatch request) {
        RbacViews.PermissionView current = permission(sysId, permissionId);
        if (request.name() == null && request.parentId() == null && request.routePath() == null
                && request.sortOrder() == null && request.status() == null) {
            throw ApiException.badRequest("EMPTY_PATCH", "至少提供一个需要修改的字段");
        }
        String name = request.name() == null ? current.name() : request.name();
        Long parentId = request.parentId() == null ? parseNullable(current.parentId()) : parseNullable(request.parentId());
        String routePath = request.routePath() == null ? current.routePath() : request.routePath();
        int sortOrder = request.sortOrder() == null ? current.sortOrder() : request.sortOrder();
        String status = request.status() == null ? current.status() : request.status();
        validatePermissionShape(sysId, current.type(), routePath, parentId, permissionId);
        jdbc.update("""
                UPDATE iam_permission
                   SET name = ?, parent_id = ?, route_path = ?, sort_order = ?, status = ?,
                       version = version + 1, updated_at = CURRENT_TIMESTAMP
                 WHERE sys_id = ? AND id = ?
                """, name, parentId, routePath, sortOrder, status, sysId, permissionId);
        bumpPolicy(sysId);
        RbacViews.PermissionView updated = permission(sysId, permissionId);
        auditService.write(context, "PERMISSION_UPDATED", "PERMISSION", Long.toString(permissionId),
                current, updated);
        return updated;
    }

    @Transactional
    public void deletePermission(SessionContext context, long sysId, long permissionId) {
        RbacViews.PermissionView current = permission(sysId, permissionId);
        Integer grants = jdbc.queryForObject("""
                SELECT COUNT(*) FROM iam_role_permission WHERE sys_id = ? AND permission_id = ?
                """, Integer.class, sysId, permissionId);
        Integer children = jdbc.queryForObject("""
                SELECT COUNT(*) FROM iam_permission WHERE sys_id = ? AND parent_id = ?
                """, Integer.class, sysId, permissionId);
        if ((grants != null && grants > 0) || (children != null && children > 0)) {
            throw ApiException.conflict("PERMISSION_IN_USE", "权限仍被角色或子权限引用，不能删除");
        }
        jdbc.update("DELETE FROM iam_permission WHERE sys_id = ? AND id = ?", sysId, permissionId);
        bumpPolicy(sysId);
        auditService.write(context, "PERMISSION_DELETED", "PERMISSION", Long.toString(permissionId),
                current, null);
    }

    public List<RbacViews.MemberView> members(long sysId) {
        return jdbc.query("""
                SELECT m.user_id, m.sys_id, u.username, m.display_name, m.status,
                       u.status AS account_status, m.version, m.created_at
                  FROM iam_member m
                  JOIN iam_user u ON u.id = m.user_id
                 WHERE m.sys_id = ?
                 ORDER BY u.username
                """, (rs, rowNum) -> memberView(rs), sysId);
    }

    public RbacViews.RoleIdsView memberRoles(SessionContext context, long sysId, long userId) {
        ensureMember(sysId, userId);
        long version = memberVersion(sysId, userId);
        List<String> selected = roleIdsForMember(sysId, userId);
        return new RbacViews.RoleIdsView(selected, Long.toString(version), roleOptions(context, sysId));
    }

    @Transactional
    public RbacViews.RoleIdsView replaceMemberRoles(SessionContext context, long sysId, long userId,
                                                    List<String> requestedIds, long expectedVersion) {
        ensureMember(sysId, userId);
        long currentVersion = memberVersion(sysId, userId);
        if (currentVersion != expectedVersion) {
            throw ApiException.preconditionFailed("成员版本已变化，请刷新后重试");
        }
        List<Long> desired = normalizeIds(requestedIds);
        List<String> before = roleIdsForMember(sysId, userId);
        validateRoleIds(context, sysId, desired);
        List<String> desiredStrings = desired.stream().map(id -> Long.toString(id)).toList();
        if (before.equals(desiredStrings)) {
            return new RbacViews.RoleIdsView(before, Long.toString(currentVersion), roleOptions(context, sysId));
        }
        jdbc.update("DELETE FROM iam_user_role WHERE sys_id = ? AND user_id = ?", sysId, userId);
        for (Long roleId : desired) {
            jdbc.update("""
                    INSERT INTO iam_user_role(sys_id,user_id,role_id,created_by)
                    VALUES (?,?,?,?)
                    """, sysId, userId, roleId, context.userId());
        }
        long newVersion = currentVersion + 1;
        jdbc.update("""
                UPDATE iam_member SET version = ?, updated_at = CURRENT_TIMESTAMP
                 WHERE sys_id = ? AND user_id = ?
                """, newVersion, sysId, userId);
        bumpPolicy(sysId);
        auditService.write(context, "MEMBER_ROLES_REPLACED", "MEMBER", Long.toString(userId),
                java.util.Map.of("roleIds", before), java.util.Map.of("roleIds", desiredStrings));
        return new RbacViews.RoleIdsView(desiredStrings, Long.toString(newVersion), roleOptions(context, sysId));
    }

    public RbacViews.PermissionIdsView rolePermissions(SessionContext context, long sysId, long roleId) {
        RbacViews.RoleView role = role(sysId, roleId);
        List<String> selected = permissionIdsForRole(sysId, roleId);
        return new RbacViews.PermissionIdsView(selected, role.version(), permissionOptions(sysId));
    }

    @Transactional
    public RbacViews.PermissionIdsView replaceRolePermissions(SessionContext context, long sysId, long roleId,
                                                              List<String> requestedIds, long expectedVersion) {
        RbacViews.RoleView role = role(sysId, roleId);
        if (role.builtin()) {
            throw ApiException.conflict("BUILTIN_ROLE_PROTECTED", "内置角色权限不可修改");
        }
        long currentVersion = Long.parseLong(role.version());
        if (currentVersion != expectedVersion) {
            throw ApiException.preconditionFailed("角色版本已变化，请刷新后重试");
        }
        List<Long> desired = normalizeIds(requestedIds);
        validatePermissionIds(sysId, desired);
        List<String> before = permissionIdsForRole(sysId, roleId);
        List<String> desiredStrings = desired.stream().map(id -> Long.toString(id)).toList();
        if (before.equals(desiredStrings)) {
            return new RbacViews.PermissionIdsView(before, role.version(), permissionOptions(sysId));
        }
        jdbc.update("DELETE FROM iam_role_permission WHERE sys_id = ? AND role_id = ?", sysId, roleId);
        for (Long permissionId : desired) {
            jdbc.update("""
                    INSERT INTO iam_role_permission(sys_id,role_id,permission_id,created_by)
                    VALUES (?,?,?,?)
                    """, sysId, roleId, permissionId, context.userId());
        }
        long newVersion = currentVersion + 1;
        jdbc.update("""
                UPDATE iam_role SET version = ?, updated_at = CURRENT_TIMESTAMP
                 WHERE sys_id = ? AND id = ?
                """, newVersion, sysId, roleId);
        bumpPolicy(sysId);
        auditService.write(context, "ROLE_PERMISSIONS_REPLACED", "ROLE", Long.toString(roleId),
                java.util.Map.of("permissionIds", before), java.util.Map.of("permissionIds", desiredStrings));
        return new RbacViews.PermissionIdsView(desiredStrings, Long.toString(newVersion),
                permissionOptions(sysId));
    }

    public List<RbacViews.AuditView> auditLogs(long sysId, int limit) {
        return auditService.list(sysId, limit);
    }

    private void validatePermissionShape(long sysId, String type, String routePath, Long parentId,
                                         Long selfId) {
        if ("MENU".equals(type)) {
            if (routePath == null || !ROUTES.contains(routePath)) {
                throw ApiException.badRequest("INVALID_ROUTE", "MENU 必须使用受控 routePath");
            }
        } else if ("ACTION".equals(type)) {
            if (routePath != null && !routePath.isBlank()) {
                throw ApiException.badRequest("INVALID_ROUTE", "ACTION 不允许设置 routePath");
            }
        } else {
            throw ApiException.badRequest("INVALID_PERMISSION_TYPE", "权限类型必须是 MENU 或 ACTION");
        }
        if (parentId != null) {
            if (selfId != null && parentId.equals(selfId)) {
                throw ApiException.badRequest("PERMISSION_CYCLE", "父权限不能是自身");
            }
            List<String> types = jdbc.query("""
                    SELECT type FROM iam_permission WHERE sys_id = ? AND id = ?
                    """, (rs, rowNum) -> rs.getString(1), sysId, parentId);
            if (types.isEmpty() || !"MENU".equals(types.getFirst())) {
                throw ApiException.badRequest("INVALID_PARENT", "父权限必须是本系统 MENU");
            }
            ensureNoCycle(sysId, parentId, selfId);
        }
    }

    private void ensureNoCycle(long sysId, long parentId, Long selfId) {
        if (selfId == null) return;
        Set<Long> seen = new HashSet<>();
        Long cursor = parentId;
        while (cursor != null && seen.add(cursor)) {
            if (cursor.equals(selfId)) {
                throw ApiException.badRequest("PERMISSION_CYCLE", "权限父子关系不能成环");
            }
            List<Long> parents = jdbc.query(
                    "SELECT parent_id FROM iam_permission WHERE sys_id = ? AND id = ?",
                    (rs, rowNum) -> rs.getObject(1, Long.class), sysId, cursor);
            cursor = parents.isEmpty() ? null : parents.getFirst();
        }
    }

    private void validateRoleIds(SessionContext context, long sysId, List<Long> roleIds) {
        for (Long roleId : roleIds) {
            List<Object[]> rows = jdbc.query("""
                    SELECT status, builtin FROM iam_role WHERE sys_id = ? AND id = ?
                    """, (rs, rowNum) -> new Object[]{rs.getString(1), rs.getBoolean(2)}, sysId, roleId);
            if (rows.isEmpty()) {
                throw ApiException.badRequest("INVALID_ROLE_SET", "角色集合包含未知或异系统 ID");
            }
            if (!"ENABLED".equals(rows.getFirst()[0])) {
                throw ApiException.badRequest("INVALID_ROLE_SET", "不能指派停用角色");
            }
            if (Boolean.TRUE.equals(rows.getFirst()[1]) && !context.platformAdmin()) {
                throw ApiException.forbidden("内置 SYS_ADMIN 只能由平台管理员指派");
            }
        }
    }

    private void validatePermissionIds(long sysId, List<Long> permissionIds) {
        for (Long permissionId : permissionIds) {
            List<String> statuses = jdbc.query("""
                    SELECT status FROM iam_permission WHERE sys_id = ? AND id = ?
                    """, (rs, rowNum) -> rs.getString(1), sysId, permissionId);
            if (statuses.isEmpty()) {
                throw ApiException.badRequest("INVALID_PERMISSION_SET", "???????????? ID");
            }
            if (!"ENABLED".equals(statuses.getFirst())) {
                throw ApiException.badRequest("INVALID_PERMISSION_SET", "????????");
            }
        }
    }

    private List<RbacViews.AssignmentRoleOption> roleOptions(SessionContext context, long sysId) {
        return jdbc.query("""
                SELECT id, code, name, status, builtin FROM iam_role
                 WHERE sys_id = ? ORDER BY code
                """, (rs, rowNum) -> {
            String status = rs.getString("status");
            boolean builtin = rs.getBoolean("builtin");
            boolean assignable = "ENABLED".equals(status) && (!builtin || context.platformAdmin());
            String reason = assignable ? "" : (!"ENABLED".equals(status) ? "角色已停用" : "内置角色仅平台管理员可指派");
            return new RbacViews.AssignmentRoleOption(Long.toString(rs.getLong("id")),
                    rs.getString("code"), rs.getString("name"), status, builtin, assignable, reason);
        }, sysId);
    }

    private List<RbacViews.AssignmentPermissionOption> permissionOptions(long sysId) {
        return jdbc.query("""
                SELECT id, code, name, type, parent_id, status FROM iam_permission
                 WHERE sys_id = ? ORDER BY sort_order, id
                """, (rs, rowNum) -> {
            String status = rs.getString("status");
            boolean grantable = "ENABLED".equals(status);
            return new RbacViews.AssignmentPermissionOption(
                    Long.toString(rs.getLong("id")),
                    rs.getString("code"),
                    rs.getString("name"),
                    rs.getString("type"),
                    rs.getObject("parent_id") == null ? null : Long.toString(rs.getLong("parent_id")),
                    status,
                    grantable,
                    grantable ? "" : "权限已停用");
        }, sysId);
    }

    private List<String> roleIdsForMember(long sysId, long userId) {
        return normalizeStrings(jdbc.query("""
                SELECT role_id FROM iam_user_role WHERE sys_id = ? AND user_id = ?
                """, (rs, rowNum) -> Long.toString(rs.getLong(1)), sysId, userId));
    }

    private List<String> permissionIdsForRole(long sysId, long roleId) {
        return normalizeStrings(jdbc.query("""
                SELECT permission_id FROM iam_role_permission WHERE sys_id = ? AND role_id = ?
                """, (rs, rowNum) -> Long.toString(rs.getLong(1)), sysId, roleId));
    }

    private void ensureMember(long sysId, long userId) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM iam_member WHERE sys_id = ? AND user_id = ?
                """, Integer.class, sysId, userId);
        if (count == null || count == 0) throw ApiException.notFound("成员不存在");
    }

    private long memberVersion(long sysId, long userId) {
        List<Long> values = jdbc.query("""
                SELECT version FROM iam_member WHERE sys_id = ? AND user_id = ?
                """, (rs, rowNum) -> rs.getLong(1), sysId, userId);
        if (values.isEmpty()) throw ApiException.notFound("成员不存在");
        return values.getFirst();
    }

    private void bumpPolicy(long sysId) {
        jdbc.update("UPDATE iam_system SET policy_version = policy_version + 1 WHERE id = ?", sysId);
    }

    private static List<Long> normalizeIds(List<String> values) {
        if (values == null) throw ApiException.badRequest("INVALID_ID_SET", "ID 集合不能为空");
        List<Long> ids = new ArrayList<>(new LinkedHashSet<>(values.stream().map(RbacService::parseId).toList()));
        ids.sort(Comparator.naturalOrder());
        return ids;
    }

    private static List<String> normalizeStrings(List<String> values) {
        List<String> result = new ArrayList<>(new LinkedHashSet<>(values));
        result.sort(Comparator.comparingLong(Long::parseLong));
        return result;
    }

    private static Long parseNullable(String value) {
        return value == null || value.isBlank() ? null : parseId(value);
    }

    private static long parseId(String value) {
        try {
            long id = Long.parseLong(value);
            if (id <= 0) throw new NumberFormatException();
            return id;
        } catch (NumberFormatException ex) {
            throw ApiException.badRequest("INVALID_ID", "ID 必须是正整数");
        }
    }

    private static RbacViews.RoleView roleView(java.sql.ResultSet rs) throws java.sql.SQLException {
        Timestamp createdAt = rs.getTimestamp("created_at");
        return new RbacViews.RoleView(
                Long.toString(rs.getLong("id")), Long.toString(rs.getLong("sys_id")),
                rs.getString("code"), rs.getString("name"), rs.getString("description"),
                rs.getString("status"), rs.getBoolean("builtin"), Long.toString(rs.getLong("version")),
                createdAt.toInstant());
    }

    private static RbacViews.PermissionView permissionView(java.sql.ResultSet rs) throws java.sql.SQLException {
        Timestamp createdAt = rs.getTimestamp("created_at");
        return new RbacViews.PermissionView(
                Long.toString(rs.getLong("id")), Long.toString(rs.getLong("sys_id")),
                rs.getString("code"), rs.getString("name"), rs.getString("type"),
                rs.getObject("parent_id") == null ? null : Long.toString(rs.getLong("parent_id")),
                rs.getString("route_path"), rs.getInt("sort_order"), rs.getString("status"),
                Long.toString(rs.getLong("version")), createdAt.toInstant());
    }

    private RbacViews.MemberView memberView(java.sql.ResultSet rs) throws java.sql.SQLException {
        long sysId = rs.getLong("sys_id");
        long userId = rs.getLong("user_id");
        List<String> roleIds = roleIdsForMember(sysId, userId);
        java.util.Map<String, String> roleNameById = new java.util.HashMap<>();
        if (!roleIds.isEmpty()) {
            org.springframework.jdbc.core.RowCallbackHandler handler = row ->
                    roleNameById.put(Long.toString(row.getLong("id")), row.getString("name"));
            jdbc.query("""
                    SELECT id, name FROM iam_role WHERE sys_id = ? AND id IN (%s)
                    """.formatted(placeholders(roleIds.size())), handler,
                    java.util.stream.Stream.concat(java.util.stream.Stream.of(sysId),
                            roleIds.stream().map(Long::parseLong)).toArray());
        }
        List<String> roleNames = roleIds.stream().map(id -> roleNameById.getOrDefault(id, "")).toList();
        return new RbacViews.MemberView(
                Long.toString(userId), Long.toString(sysId), rs.getString("username"),
                rs.getString("display_name"), rs.getString("status"), rs.getString("account_status"),
                roleIds, roleNames, Long.toString(rs.getLong("version")),
                rs.getTimestamp("created_at").toInstant());
    }

    private static String placeholders(int count) {
        return String.join(",", java.util.Collections.nCopies(count, "?"));
    }
}
