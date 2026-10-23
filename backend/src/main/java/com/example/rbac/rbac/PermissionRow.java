package com.example.rbac.rbac;

import java.util.List;

public record PermissionRow(
        long id,
        Long parentId,
        String code,
        String name,
        String type,
        String routePath,
        int sortOrder) {
}
