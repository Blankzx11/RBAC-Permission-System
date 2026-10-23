package com.example.rbac.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record LoginRequest(
        @NotBlank @Pattern(regexp = "^[a-z0-9_]{3,32}$", message = "用户名必须为3-32位小写字母、数字或下划线")
        String username,
        @NotBlank String password,
        @Pattern(regexp = "^[1-9][0-9]{0,18}$", message = "sysId 必须是正整数字符串")
        String sysId) {
}
