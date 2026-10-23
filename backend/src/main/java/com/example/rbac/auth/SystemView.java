package com.example.rbac.auth;

import java.time.Instant;

public record SystemView(
        String id,
        String code,
        String name,
        String description,
        String status,
        String policyVersion,
        String version,
        Instant createdAt) {
}
