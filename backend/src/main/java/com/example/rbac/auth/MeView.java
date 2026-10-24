package com.example.rbac.auth;

import java.time.Instant;
import java.util.List;

public record MeView(
        String userId,
        String username,
        String displayName,
        String sysId,
        boolean platformAdmin,
        List<String> roleIds,
        String policyVersion) {
}
