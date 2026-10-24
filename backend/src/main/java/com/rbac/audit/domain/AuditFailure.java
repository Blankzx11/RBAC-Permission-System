package com.rbac.audit.domain;

public class AuditFailure extends RuntimeException {
    private final int status;
    private final String code;

    public AuditFailure(int status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public int status() { return status; }
    public String code() { return code; }

    public static AuditFailure invalid(String message) {
        return new AuditFailure(422, "VALIDATION_ERROR", message);
    }

    public static AuditFailure forbidden(String message) {
        return new AuditFailure(403, "FORBIDDEN", message);
    }
}
