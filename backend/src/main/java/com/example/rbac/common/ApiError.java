package com.example.rbac.common;

import java.util.List;

public record ApiError(String code, String message, String requestId, List<String> details) {
}
