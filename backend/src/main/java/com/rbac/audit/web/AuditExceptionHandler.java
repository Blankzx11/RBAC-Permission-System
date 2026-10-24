package com.rbac.audit.web;

import com.rbac.audit.domain.*;
import com.rbac.audit.port.SecurityEventRecorder;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.Map;

@RestControllerAdvice
public class AuditExceptionHandler {
    private final SecurityEventRecorder recorder;
    private final CurrentAuditActor actors;

    public AuditExceptionHandler(SecurityEventRecorder recorder, CurrentAuditActor actors) { this.recorder = recorder; this.actors = actors; }

    @ExceptionHandler(AuditFailure.class)
    public ResponseEntity<ApiModels.Error> business(AuditFailure failure, HttpServletRequest request) {
        if (failure.status() == 403) {
            AuditActor actor = actors.current(request);
            var reason = failure.code().equals("SYSTEM_CONTEXT_MISMATCH") ? SecurityEvent.Reason.CONTEXT_MISMATCH : SecurityEvent.Reason.FORBIDDEN;
            recorder.recordSecurityEvent(new SecurityEvent(actor.userId(), actor.displayName(), actor.sysId(), reason, actor.requestId(), actor.ip()));
        }
        return ResponseEntity.status(failure.status()).body(new ApiModels.Error(failure.code(), failure.getMessage(), RequestContextFilter.requestId(request), Map.of()));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiModels.Error> databaseUnavailable(HttpServletRequest request) {
        return ResponseEntity.status(503).body(new ApiModels.Error("SERVICE_UNAVAILABLE", "日志存储暂不可用", RequestContextFilter.requestId(request), Map.of()));
    }
}
