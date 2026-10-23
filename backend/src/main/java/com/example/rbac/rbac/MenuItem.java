package com.example.rbac.rbac;

import java.util.List;

public record MenuItem(
        String id,
        String parentId,
        String name,
        String routePath,
        boolean navigable,
        List<MenuItem> children) {
}
