package com.example.rbac.rbac;

import com.example.rbac.auth.AuthService;
import com.example.rbac.auth.SystemView;
import com.example.rbac.common.ApiResponse;
import com.example.rbac.common.Etag;
import com.example.rbac.common.RequestIdFilter;
import com.example.rbac.security.RequirePermission;
import com.example.rbac.security.SessionContext;
import com.example.rbac.security.SessionGate;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class RbacController {
    private final RbacService service;
    private final SessionGate sessionGate;
    private final AuthService authService;

    public RbacController(RbacService service, SessionGate sessionGate, AuthService authService) {
        this.service = service;
        this.sessionGate = sessionGate;
        this.authService = authService;
    }

    @GetMapping("/platform/systems")
    public ApiResponse<List<SystemView>> platformSystems(HttpServletRequest request) {
        return ok(authService.systems(sessionGate.require(request)), request);
    }

    @GetMapping("/systems/{sysId}/members")
    @RequirePermission("sys:member:read")
    public ApiResponse<List<RbacViews.MemberView>> members(@PathVariable long sysId,
                                                            HttpServletRequest request) {
        return ok(service.members(sysId), request);
    }

    @GetMapping("/systems/{sysId}/members/{userId}/roles")
    @RequirePermission("sys:member:assign")
    public ResponseEntity<ApiResponse<RbacViews.RoleIdsView>> memberRoles(@PathVariable long sysId,
                                                                          @PathVariable long userId,
                                                                          HttpServletRequest request) {
        RbacViews.RoleIdsView view = service.memberRoles(sessionGate.require(request), sysId, userId);
        return etag(ok(view, request), view.version());
    }

    @PutMapping("/systems/{sysId}/members/{userId}/roles")
    @RequirePermission("sys:member:assign")
    public ResponseEntity<ApiResponse<RbacViews.RoleIdsView>> replaceMemberRoles(
            @PathVariable long sysId,
            @PathVariable long userId,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody RbacRequests.IdsRequest body,
            HttpServletRequest request) {
        long expected = Etag.require(ifMatch);
        RbacViews.RoleIdsView view = service.replaceMemberRoles(
                sessionGate.require(request), sysId, userId, body.roleIds(), expected);
        return etag(ok(view, request), view.version());
    }

    @GetMapping("/systems/{sysId}/roles")
    @RequirePermission("sys:role:read")
    public ApiResponse<List<RbacViews.RoleView>> roles(@PathVariable long sysId,
                                                       HttpServletRequest request) {
        return ok(service.roles(sysId), request);
    }

    @GetMapping("/systems/{sysId}/roles/{roleId}")
    @RequirePermission("sys:role:read")
    public ApiResponse<RbacViews.RoleView> role(@PathVariable long sysId, @PathVariable long roleId,
                                                HttpServletRequest request) {
        return ok(service.role(sysId, roleId), request);
    }

    @PostMapping("/systems/{sysId}/roles")
    @RequirePermission("sys:role:create")
    public ResponseEntity<ApiResponse<RbacViews.RoleView>> createRole(
            @PathVariable long sysId,
            @Valid @RequestBody RbacRequests.RoleCreate body,
            HttpServletRequest request) {
        RbacViews.RoleView view = service.createRole(sessionGate.require(request), body);
        return ResponseEntity.status(HttpStatus.CREATED).eTag(Etag.value(Long.parseLong(view.version())))
                .body(ok(view, request));
    }

    @PatchMapping("/systems/{sysId}/roles/{roleId}")
    @RequirePermission("sys:role:update")
    public ResponseEntity<ApiResponse<RbacViews.RoleView>> updateRole(
            @PathVariable long sysId,
            @PathVariable long roleId,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody RbacRequests.RolePatch body,
            HttpServletRequest request) {
        long expected = Etag.require(ifMatch);
        if (expected != Long.parseLong(service.role(sysId, roleId).version())) {
            throw com.example.rbac.common.ApiException.preconditionFailed("角色版本已变化，请刷新后重试");
        }
        RbacViews.RoleView view = service.updateRole(sessionGate.require(request), sysId, roleId, body);
        return etag(ok(view, request), view.version());
    }

    @DeleteMapping("/systems/{sysId}/roles/{roleId}")
    @RequirePermission("sys:role:delete")
    public ApiResponse<Object> deleteRole(@PathVariable long sysId, @PathVariable long roleId,
                                          HttpServletRequest request) {
        service.deleteRole(sessionGate.require(request), sysId, roleId);
        return ok(java.util.Map.of(), request);
    }

    @GetMapping("/systems/{sysId}/permissions")
    @RequirePermission("sys:permission:read")
    public ApiResponse<List<RbacViews.PermissionView>> permissions(@PathVariable long sysId,
                                                                   HttpServletRequest request) {
        return ok(service.permissions(sysId), request);
    }

    @GetMapping("/systems/{sysId}/permissions/{permissionId}")
    @RequirePermission("sys:permission:read")
    public ApiResponse<RbacViews.PermissionView> permission(@PathVariable long sysId,
                                                            @PathVariable long permissionId,
                                                            HttpServletRequest request) {
        return ok(service.permission(sysId, permissionId), request);
    }

    @PostMapping("/systems/{sysId}/permissions")
    @RequirePermission("sys:permission:create")
    public ResponseEntity<ApiResponse<RbacViews.PermissionView>> createPermission(
            @PathVariable long sysId,
            @Valid @RequestBody RbacRequests.PermissionCreate body,
            HttpServletRequest request) {
        RbacViews.PermissionView view = service.createPermission(sessionGate.require(request), body);
        return ResponseEntity.status(HttpStatus.CREATED)
                .eTag(Etag.value(Long.parseLong(view.version()))).body(ok(view, request));
    }

    @PatchMapping("/systems/{sysId}/permissions/{permissionId}")
    @RequirePermission("sys:permission:update")
    public ResponseEntity<ApiResponse<RbacViews.PermissionView>> updatePermission(
            @PathVariable long sysId,
            @PathVariable long permissionId,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody RbacRequests.PermissionPatch body,
            HttpServletRequest request) {
        long expected = Etag.require(ifMatch);
        if (expected != Long.parseLong(service.permission(sysId, permissionId).version())) {
            throw com.example.rbac.common.ApiException.preconditionFailed("权限版本已变化，请刷新后重试");
        }
        RbacViews.PermissionView view = service.updatePermission(
                sessionGate.require(request), sysId, permissionId, body);
        return etag(ok(view, request), view.version());
    }

    @DeleteMapping("/systems/{sysId}/permissions/{permissionId}")
    @RequirePermission("sys:permission:delete")
    public ApiResponse<Object> deletePermission(@PathVariable long sysId,
                                                @PathVariable long permissionId,
                                                HttpServletRequest request) {
        service.deletePermission(sessionGate.require(request), sysId, permissionId);
        return ok(java.util.Map.of(), request);
    }

    @GetMapping("/systems/{sysId}/roles/{roleId}/permissions")
    @RequirePermission("sys:role:grant")
    public ResponseEntity<ApiResponse<RbacViews.PermissionIdsView>> rolePermissions(
            @PathVariable long sysId,
            @PathVariable long roleId,
            HttpServletRequest request) {
        RbacViews.PermissionIdsView view = service.rolePermissions(
                sessionGate.require(request), sysId, roleId);
        return etag(ok(view, request), view.version());
    }

    @PutMapping("/systems/{sysId}/roles/{roleId}/permissions")
    @RequirePermission("sys:role:grant")
    public ResponseEntity<ApiResponse<RbacViews.PermissionIdsView>> replaceRolePermissions(
            @PathVariable long sysId,
            @PathVariable long roleId,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody RbacRequests.PermissionIdsRequest body,
            HttpServletRequest request) {
        long expected = Etag.require(ifMatch);
        RbacViews.PermissionIdsView view = service.replaceRolePermissions(
                sessionGate.require(request), sysId, roleId, body.permissionIds(), expected);
        return etag(ok(view, request), view.version());
    }

    @GetMapping("/systems/{sysId}/audit-logs")
    @RequirePermission("sys:audit:read")
    public ApiResponse<List<RbacViews.AuditView>> auditLogs(
            @PathVariable long sysId,
            @RequestParam(defaultValue = "50") int limit,
            HttpServletRequest request) {
        return ok(service.auditLogs(sysId, limit), request);
    }

    private static <T> ApiResponse<T> ok(T data, HttpServletRequest request) {
        return ApiResponse.of(data, RequestIdFilter.get(request));
    }

    private static <T> ResponseEntity<ApiResponse<T>> etag(ApiResponse<T> body, String version) {
        return ResponseEntity.ok().eTag(Etag.value(Long.parseLong(version))).body(body);
    }
}
