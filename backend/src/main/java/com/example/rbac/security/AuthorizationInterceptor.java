package com.example.rbac.security;

import com.example.rbac.common.ApiException;
import com.example.rbac.rbac.PermissionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

@Component
public class AuthorizationInterceptor implements HandlerInterceptor {
    private final SessionGate sessionGate;
    private final PermissionService permissionService;

    public AuthorizationInterceptor(SessionGate sessionGate, PermissionService permissionService) {
        this.sessionGate = sessionGate;
        this.permissionService = permissionService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod method)) {
            return true;
        }
        SessionContext context = sessionGate.authenticate(request);

        if (request.getRequestURI().startsWith("/api/v1/platform/") && !context.platformAdmin()) {
            throw ApiException.forbidden("仅平台管理员可访问该接口");
        }

        Object variables = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (variables instanceof Map<?, ?> pathVariables && pathVariables.get("sysId") != null) {
            String pathSysId = pathVariables.get("sysId").toString();
            if (context.sysId() == null || !context.sysIdString().equals(pathSysId)) {
                throw ApiException.forbidden("路径系统与会话上下文不一致");
            }
        }

        RequirePermission required = AnnotatedElementUtils.findMergedAnnotation(method.getMethod(), RequirePermission.class);
        if (required == null) {
            required = AnnotatedElementUtils.findMergedAnnotation(method.getBeanType(), RequirePermission.class);
        }
        if (required != null && !permissionService.authorize(context, required.value())) {
            throw ApiException.forbidden("缺少权限：" + required.value());
        }
        return true;
    }
}
