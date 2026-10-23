package com.example.rbac.auth;

public record TokenView(
        String accessToken,
        String tokenType,
        long expiresIn,
        String sessionId,
        String sysId) {
}
