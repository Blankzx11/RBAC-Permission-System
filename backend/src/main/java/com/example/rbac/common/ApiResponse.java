package com.example.rbac.common;

public record ApiResponse<T>(T data, String requestId) {
    public static <T> ApiResponse<T> of(T data, String requestId) {
        return new ApiResponse<>(data, requestId);
    }
}
