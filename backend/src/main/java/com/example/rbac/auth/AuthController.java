package com.example.rbac.auth;

import com.example.rbac.common.ApiResponse;
import com.example.rbac.common.RequestIdFilter;
import com.example.rbac.rbac.EffectivePermissions;
import com.example.rbac.rbac.MenuItem;
import com.example.rbac.rbac.PermissionService;
import com.example.rbac.security.SessionContext;
import com.example.rbac.security.SessionGate;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class AuthController {
    private final AuthService authService;
    private final SessionGate sessionGate;
    private final PermissionService permissionService;

    public AuthController(AuthService authService, SessionGate sessionGate, PermissionService permissionService) {
        this.authService = authService;
        this.sessionGate = sessionGate;
        this.permissionService = permissionService;
    }

    @PostMapping("/auth/sessions")
    public ApiResponse<TokenView> login(@Valid @RequestBody LoginRequest request,
                                        HttpServletRequest httpRequest) {
        return ApiResponse.of(authService.login(request, httpRequest), RequestIdFilter.get(httpRequest));
    }

    @PostMapping("/auth/contexts")
    public ApiResponse<TokenView> switchContext(@Valid @RequestBody ContextRequest request,
                                                HttpServletRequest httpRequest) {
        SessionContext current = sessionGate.require(httpRequest);
        return ApiResponse.of(authService.switchContext(current, request, httpRequest),
                RequestIdFilter.get(httpRequest));
    }

    @DeleteMapping("/auth/sessions/current")
    public ApiResponse<Object> logout(HttpServletRequest httpRequest) {
        authService.logout(sessionGate.require(httpRequest));
        return ApiResponse.of(MapHolder.EMPTY, RequestIdFilter.get(httpRequest));
    }

    @GetMapping("/me")
    public ApiResponse<MeView> me(HttpServletRequest request) {
        return ApiResponse.of(authService.me(sessionGate.require(request)), RequestIdFilter.get(request));
    }

    @GetMapping("/me/systems")
    public ApiResponse<List<SystemView>> systems(HttpServletRequest request) {
        return ApiResponse.of(authService.systems(sessionGate.require(request)), RequestIdFilter.get(request));
    }

    @GetMapping("/me/permissions")
    public ApiResponse<EffectivePermissions> permissions(HttpServletRequest request) {
        return ApiResponse.of(permissionService.resolve(sessionGate.require(request)),
                RequestIdFilter.get(request));
    }

    @GetMapping("/me/menus")
    public ApiResponse<List<MenuItem>> menus(HttpServletRequest request) {
        return ApiResponse.of(permissionService.buildMenus(sessionGate.require(request)),
                RequestIdFilter.get(request));
    }

    private static final class MapHolder {
        static final java.util.Map<String, Object> EMPTY = java.util.Map.of();
    }
}
