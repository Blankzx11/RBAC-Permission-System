package com.example.rbac.rbac;

import java.util.List;

public record EffectivePermissions(
        String sysId,
        String userId,
        String policyVersion,
        List<String> permissionCodes,
        List<String> directRoleIds) {
}
