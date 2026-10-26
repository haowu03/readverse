package com.readverse.common.result;

import java.util.UUID;

/**
 * Unified API response.
 */
public record R<T>(int code, String message, T data, String traceId) {

    public static <T> R<T> ok(T data) {
        return new R<>(0, "ok", data, newTraceId());
    }

    public static <T> R<T> ok() {
        return ok(null);
    }

    public static <T> R<T> fail(int code, String message) {
        return new R<>(code, message, null, newTraceId());
    }

    public static <T> R<T> fail(com.readverse.common.exception.ErrorCode errorCode) {
        return new R<>(errorCode.getCode(), errorCode.getMessage(), null, newTraceId());
    }

    private static String newTraceId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
