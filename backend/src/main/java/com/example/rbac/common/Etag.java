package com.example.rbac.common;

public final class Etag {
    private Etag() {
    }

    public static long require(String header) {
        if (header == null || header.isBlank()) {
            throw ApiException.preconditionRequired("必须提供 If-Match 版本头");
        }
        String value = header.trim();
        if (!value.startsWith("\"") || !value.endsWith("\"") || value.length() < 3) {
            throw ApiException.badRequest("INVALID_ETAG", "If-Match 必须是强 ETag，例如 \"7\"");
        }
        try {
            long parsed = Long.parseLong(value.substring(1, value.length() - 1));
            if (parsed <= 0) throw new NumberFormatException();
            return parsed;
        } catch (NumberFormatException ex) {
            throw ApiException.badRequest("INVALID_ETAG", "If-Match 版本不合法");
        }
    }

    public static String value(long version) {
        return "\"" + version + "\"";
    }
}
