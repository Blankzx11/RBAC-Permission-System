package com.rbac.audit.web;

import com.rbac.audit.domain.*;
import com.rbac.audit.functional.AuditFunctions;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Set;

public final class AuditParameters {
    private static final Set<String> COMMON = Set.of("from", "to", "actorId", "action", "page", "pageSize");
    private AuditParameters() { }

    public static AuditQuery.Input query(HttpServletRequest request, boolean platform) {
        request.getParameterMap().forEach((key, values) -> {
            if ((!COMMON.contains(key) && !(platform && key.equals("sysId"))) || values.length != 1)
                throw new AuditFailure(400, "BAD_REQUEST", "查询包含未知字段或重复参数");
        });
        return new AuditQuery.Input(instant(request.getParameter("from")), instant(request.getParameter("to")),
            optionalId(request.getParameter("actorId"), "actorId"), request.getParameter("action"),
            number(request.getParameter("page"), 1), number(request.getParameter("pageSize"), 20));
    }

    public static Long optionalId(String value, String field) { return value == null ? null : AuditFunctions.positiveId(value, field); }
    public static long id(String value, String field) { return AuditFunctions.positiveId(value, field); }
    public static void detail(HttpServletRequest request) {
        if (!request.getParameterMap().isEmpty()) throw new AuditFailure(400, "BAD_REQUEST", "详情接口不接受查询参数");
    }

    private static int number(String value, int defaultValue) {
        if (value == null) return defaultValue;
        if (!value.matches("[0-9]{1,10}")) throw AuditFailure.invalid("分页参数无效");
        try { return Integer.parseInt(value); }
        catch (NumberFormatException ex) { throw AuditFailure.invalid("分页参数超出范围"); }
    }

    private static Instant instant(String value) {
        if (value == null) return null;
        try { return Instant.parse(value); }
        catch (DateTimeParseException ex) { throw AuditFailure.invalid("日期必须为带时区的ISO8601时间"); }
    }
}
