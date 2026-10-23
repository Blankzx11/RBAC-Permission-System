package com.example.rbac.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ContextRequest(
        @NotBlank @Pattern(regexp = "^[1-9][0-9]{0,18}$", message = "sysId 必须是正整数字符串")
        String sysId) {
}
