package com.example.rbac.rbac;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class RbacRequests {
    private RbacRequests() {
    }

    public record RoleCreate(
            @NotBlank @Pattern(regexp = "^[A-Z][A-Z0-9_]{1,39}$") String code,
            @NotBlank @Size(max = 64) String name,
            @Size(max = 500) String description) {
    }

    public record RolePatch(
            @Size(min = 1, max = 64) String name,
            @Size(max = 500) String description,
            @Pattern(regexp = "ENABLED|DISABLED") String status) {
    }

    public record PermissionCreate(
            @NotBlank @Pattern(regexp = "^[a-z][a-z0-9:_]{2,79}$") String code,
            @NotBlank @Size(max = 64) String name,
            @NotBlank @Pattern(regexp = "MENU|ACTION") String type,
            @Pattern(regexp = "^[1-9][0-9]{0,18}$") String parentId,
            String routePath,
            @Min(0) @Max(9999) Integer sortOrder) {
    }

    public record PermissionPatch(
            @Size(min = 1, max = 64) String name,
            @Pattern(regexp = "^[1-9][0-9]{0,18}$") String parentId,
            String routePath,
            @Min(0) @Max(9999) Integer sortOrder,
            @Pattern(regexp = "ENABLED|DISABLED") String status) {
    }

    public record IdsRequest(
            @NotNull @Size(max = 1000) java.util.List<@Pattern(regexp = "^[1-9][0-9]{0,18}$") String> roleIds) {
    }

    public record PermissionIdsRequest(
            @NotNull @Size(max = 1000) java.util.List<@Pattern(regexp = "^[1-9][0-9]{0,18}$") String> permissionIds) {
    }
}

