package com.rbac.audit.web;

import com.rbac.audit.application.AuditQueryService;
import com.rbac.audit.domain.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class AuditController {
    private final AuditQueryService service;
    private final CurrentAuditActor actors;

    public AuditController(AuditQueryService service, CurrentAuditActor actors) { this.service = service; this.actors = actors; }

    @GetMapping("/platform/audit-logs")
    public ApiModels.Success<AuditQuery.Page<ApiModels.AuditView>> platformLogs(HttpServletRequest request) {
        var actor = actors.current(request);
        var input = AuditParameters.query(request, true);
        var scope = new AuditScope.Platform(AuditParameters.optionalId(request.getParameter("sysId"), "sysId"));
        return query(actor, scope, input);
    }

    @GetMapping("/systems/{sysId}/audit-logs")
    public ApiModels.Success<AuditQuery.Page<ApiModels.AuditView>> systemLogs(@PathVariable String sysId, HttpServletRequest request) {
        var actor = actors.current(request);
        return query(actor, new AuditScope.System(AuditParameters.id(sysId, "sysId")), AuditParameters.query(request, false));
    }

    @GetMapping("/platform/audit-logs/{logId}")
    public ApiModels.Success<ApiModels.AuditView> platformDetail(@PathVariable String logId, HttpServletRequest request) {
        var actor = actors.current(request);
        AuditParameters.detail(request);
        return new ApiModels.Success<>(ApiModels.AuditView.from(service.getAuditDetail(actor, new AuditScope.Platform(null), AuditParameters.id(logId, "logId"))), actor.requestId());
    }

    @GetMapping("/systems/{sysId}/audit-logs/{logId}")
    public ApiModels.Success<ApiModels.AuditView> systemDetail(@PathVariable String sysId, @PathVariable String logId, HttpServletRequest request) {
        var actor = actors.current(request);
        AuditParameters.detail(request);
        var scope = new AuditScope.System(AuditParameters.id(sysId, "sysId"));
        return new ApiModels.Success<>(ApiModels.AuditView.from(service.getAuditDetail(actor, scope, AuditParameters.id(logId, "logId"))), actor.requestId());
    }

    private ApiModels.Success<AuditQuery.Page<ApiModels.AuditView>> query(AuditActor actor, AuditScope scope, AuditQuery.Input input) {
        var page = service.queryAudit(actor, scope, input);
        var projected = new AuditQuery.Page<>(page.items().stream().map(ApiModels.AuditView::from).toList(), page.total(), page.page(), page.pageSize());
        return new ApiModels.Success<>(projected, actor.requestId());
    }
}
