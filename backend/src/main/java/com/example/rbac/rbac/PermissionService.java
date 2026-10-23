package com.example.rbac.rbac;

import com.example.rbac.common.ApiException;
import com.example.rbac.security.SessionContext;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class PermissionService {
    private record CacheKey(long sysId, long userId, long policyVersion) {
    }

    private record Facts(List<String> codes, List<String> roleIds) {
    }

    private record CacheEntry(Facts facts, Instant expiresAt) {
    }

    private final JdbcTemplate jdbc;
    private final long cacheSeconds;
    private final ConcurrentHashMap<CacheKey, CacheEntry> cache = new ConcurrentHashMap<>();

    public PermissionService(JdbcTemplate jdbc,
                             @Value("${rbac.permission-cache-seconds:60}") long cacheSeconds) {
        this.jdbc = jdbc;
        this.cacheSeconds = cacheSeconds;
    }

    public long loadPolicyVersion(long sysId) {
        List<Long> versions = jdbc.query(
                "SELECT policy_version FROM iam_system WHERE id = ? AND status = 'ENABLED'",
                (rs, rowNum) -> rs.getLong(1), sysId);
        if (versions.isEmpty()) {
            throw ApiException.forbidden("当前子系统不存在或已停用");
        }
        return versions.getFirst();
    }

    public EffectivePermissions resolve(SessionContext context) {
        long sysId = context.requireSystemId();
        long expectedVersion = loadPolicyVersion(sysId);
        for (int attempt = 0; attempt < 2; attempt++) {
            Facts facts = loadFacts(context);
            long after = loadPolicyVersion(sysId);
            if (after == expectedVersion) {
                cache.put(new CacheKey(sysId, context.userId(), after),
                        new CacheEntry(facts, Instant.now().plusSeconds(cacheSeconds)));
                return new EffectivePermissions(
                        Long.toString(sysId),
                        Long.toString(context.userId()),
                        Long.toString(after),
                        facts.codes(),
                        facts.roleIds());
            }
            expectedVersion = after;
        }
        throw ApiException.unavailable("权限策略持续变化，请稍后重试");
    }

    public boolean authorize(SessionContext context, String permissionCode) {
        long sysId = context.requireSystemId();
        if (!permissionExists(sysId, permissionCode)) {
            return false;
        }
        if (context.platformAdmin()) {
            return true;
        }
        Facts facts = cachedFacts(context, sysId);
        return facts.codes().contains(permissionCode);
    }

    public List<MenuItem> buildMenus(SessionContext context) {
        long sysId = context.requireSystemId();
        Facts facts = cachedFacts(context, sysId);
        List<PermissionRow> allPermissions = jdbc.query("""
                SELECT id, parent_id, code, name, type, route_path, sort_order
                  FROM iam_permission
                 WHERE sys_id = ? AND status = 'ENABLED'
                 ORDER BY sort_order, id
                """, (rs, rowNum) -> new PermissionRow(
                        rs.getLong("id"),
                        rs.getObject("parent_id", Long.class),
                        rs.getString("code"),
                        rs.getString("name"),
                        rs.getString("type"),
                        rs.getString("route_path"),
                        rs.getInt("sort_order")), sysId);
        return buildMenuTree(allPermissions, new LinkedHashSet<>(facts.codes()), context.platformAdmin());
    }

    public boolean permissionExists(long sysId, String code) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM iam_permission
                 WHERE sys_id = ? AND code = ? AND status = 'ENABLED'
                """, Integer.class, sysId, code);
        return count != null && count > 0;
    }

    static List<MenuItem> buildMenuTree(List<PermissionRow> menus, Set<String> authorizedCodes,
                                        boolean platformAdmin) {
        if (menus.isEmpty()) {
            return List.of();
        }
        Map<Long, PermissionRow> byId = new HashMap<>();
        for (PermissionRow menu : menus) {
            if ("MENU".equals(menu.type())) {
                byId.put(menu.id(), menu);
            }
        }

        Set<Long> visibleIds = new HashSet<>();
        for (PermissionRow row : menus) {
            if (authorizedCodes.contains(row.code()) || platformAdmin) {
                if ("MENU".equals(row.type())) {
                    addWithAncestors(row.id(), byId, visibleIds);
                } else if (row.parentId() != null && byId.containsKey(row.parentId())) {
                    // An action grant keeps its containing menu visible as a non-navigable container.
                    addWithAncestors(row.parentId(), byId, visibleIds);
                }
            }
        }

        Map<Long, List<PermissionRow>> childrenByParent = new HashMap<>();
        List<PermissionRow> roots = new ArrayList<>();
        for (PermissionRow row : menus) {
            if (!"MENU".equals(row.type()) || !visibleIds.contains(row.id())) continue;
            Long parentId = row.parentId();
            if (parentId == null || !visibleIds.contains(parentId)) {
                roots.add(row);
            } else {
                childrenByParent.computeIfAbsent(parentId, ignored -> new ArrayList<>()).add(row);
            }
        }
        Comparator<PermissionRow> order = Comparator.comparingInt(PermissionRow::sortOrder)
                .thenComparingLong(PermissionRow::id);
        roots.sort(order);
        childrenByParent.values().forEach(list -> list.sort(order));
        return roots.stream()
                .map(row -> toMenu(row, childrenByParent, authorizedCodes, platformAdmin, new HashSet<>()))
                .toList();
    }

    private static void addWithAncestors(long id, Map<Long, PermissionRow> byId, Set<Long> visibleIds) {
        Long cursor = id;
        Set<Long> guard = new HashSet<>();
        while (cursor != null && guard.add(cursor)) {
            PermissionRow row = byId.get(cursor);
            if (row == null) break;
            visibleIds.add(row.id());
            cursor = row.parentId();
        }
    }

    private static MenuItem toMenu(PermissionRow row, Map<Long, List<PermissionRow>> childrenByParent,
                                   Set<String> authorizedCodes, boolean platformAdmin, Set<Long> path) {
        if (!path.add(row.id())) {
            throw ApiException.conflict("MENU_CYCLE", "权限菜单存在父子环");
        }
        List<MenuItem> children = childrenByParent.getOrDefault(row.id(), List.of()).stream()
                .map(child -> toMenu(child, childrenByParent, authorizedCodes, platformAdmin, path))
                .toList();
        path.remove(row.id());
        boolean navigable = platformAdmin || authorizedCodes.contains(row.code());
        return new MenuItem(
                Long.toString(row.id()),
                row.parentId() == null ? null : Long.toString(row.parentId()),
                row.name(),
                row.routePath(),
                navigable,
                children);
    }

    private Facts cachedFacts(SessionContext context, long sysId) {
        long version = loadPolicyVersion(sysId);
        CacheKey key = new CacheKey(sysId, context.userId(), version);
        CacheEntry hit = cache.get(key);
        if (hit != null && hit.expiresAt().isAfter(Instant.now())) {
            return hit.facts();
        }
        EffectivePermissions effective = resolve(context);
        return new Facts(effective.permissionCodes(), effective.directRoleIds());
    }

    private Facts loadFacts(SessionContext context) {
        long sysId = context.requireSystemId();
        try {
            List<String> roleIds = new ArrayList<>(new TreeSet<>(jdbc.query("""
                    SELECT DISTINCT ur.role_id
                      FROM iam_user_role ur
                     WHERE ur.sys_id = ? AND ur.user_id = ?
                    """, (rs, rowNum) -> Long.toString(rs.getLong(1)), sysId, context.userId())));

            List<String> codes;
            if (context.platformAdmin()) {
                codes = jdbc.query("""
                        SELECT code FROM iam_permission
                         WHERE sys_id = ? AND status = 'ENABLED'
                         ORDER BY code
                        """, (rs, rowNum) -> rs.getString(1), sysId);
            } else {
                codes = jdbc.query("""
                        SELECT DISTINCT p.code
                          FROM iam_member m
                          JOIN iam_user u ON u.id = m.user_id AND u.status = 'ENABLED'
                          JOIN iam_user_role ur ON ur.sys_id = m.sys_id AND ur.user_id = m.user_id
                          JOIN iam_role r ON r.sys_id = ur.sys_id AND r.id = ur.role_id
                                          AND r.status = 'ENABLED'
                          JOIN iam_role_permission rp ON rp.sys_id = r.sys_id AND rp.role_id = r.id
                          JOIN iam_permission p ON p.sys_id = rp.sys_id AND p.id = rp.permission_id
                                               AND p.status = 'ENABLED'
                         WHERE m.sys_id = ? AND m.user_id = ? AND m.status = 'ENABLED'
                         ORDER BY p.code
                        """, (rs, rowNum) -> rs.getString(1), sysId, context.userId());
            }
            return new Facts(List.copyOf(new LinkedHashSet<>(codes)), roleIds);
        } catch (DataAccessException ex) {
            throw ApiException.unavailable("权限事实读取失败，请稍后重试");
        }
    }
}


