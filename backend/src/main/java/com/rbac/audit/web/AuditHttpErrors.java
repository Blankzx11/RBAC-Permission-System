package com.rbac.audit.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rbac.audit.domain.SecurityEvent;
import com.rbac.audit.port.SecurityEventRecorder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.Map;

@Component
public class AuditHttpErrors {
    private final ObjectMapper mapper;
    private final SecurityEventRecorder recorder;

    public AuditHttpErrors(ObjectMapper mapper, SecurityEventRecorder recorder) { this.mapper = mapper; this.recorder = recorder; }

    public void unauthenticated(HttpServletRequest request, HttpServletResponse response, AuthenticationException ignored) throws IOException {
        write(request, response, 401, "UNAUTHENTICATED", "请先登录");
    }

    public void demoLoginFailed(HttpServletRequest request, HttpServletResponse response, AuthenticationException failure) throws IOException {
        // 不解析Authorization头，避免误把凭据复制到安全日志。
        recorder.recordSecurityEvent(new SecurityEvent(null, "unknown", null, SecurityEvent.Reason.BAD_CREDENTIALS,
            RequestContextFilter.requestId(request), request.getRemoteAddr()));
        unauthenticated(request, response, failure);
    }

    public void forbidden(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ignored) throws IOException {
        write(request, response, 403, "FORBIDDEN", "没有访问权限");
    }

    private void write(HttpServletRequest request, HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        mapper.writeValue(response.getOutputStream(), new ApiModels.Error(code, message, RequestContextFilter.requestId(request), Map.of()));
    }
}
