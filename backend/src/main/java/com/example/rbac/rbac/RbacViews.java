package com.example.rbac.rbac;

import java.time.Instant;
import java.util.List;

public final class RbacViews {
    private RbacViews() {
    }

    public record RoleView(String id, String sysId, String code, String name, String description,
                           String status, boolean builtin, String version, Instant createdAt) {
    }

    public record PermissionView(String id, String sysId, String code, String name, String type,
                                 String parentId, String routePath, int sortOrder, String status,
                                 String version, Instant createdAt) {
    }

    public record MemberView(String userId, String sysId, String username, String displayName,
                             String status, String accountStatus, List<String> roleIds,
                             List<String> roleNames, String version, Instant createdAt) {
    }

    public record AssignmentRoleOption(String id, String code, String name, String status,
                                       boolean builtin, boolean assignable, String reason) {
    }

    public record AssignmentPermissionOption(String id, String code, String name, String type,
                                             String parentId, String status, boolean grantable,
                                             String reason) {
    }

    public record RoleIdsView(List<String> roleIds, String version, List<AssignmentRoleOption> options) {
    }

    public record PermissionIdsView(List<String> permissionIds, String version,
                                    List<AssignmentPermissionOption> options) {
    }

    public record AuditView(String id, String sysId, String actorId, String actorName, String action,
                            String targetType, String targetId, String beforeJson, String afterJson,
                            String requestId, Instant createdAt) {
    }
}
