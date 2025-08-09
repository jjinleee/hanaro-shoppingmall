package com.ijin.hanaro.common.error;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        String code,
        String message,
        List<String> details,
        Instant timestamp
) {
    public static ErrorResponse of(String code, String message, List<String> details) {
        return new ErrorResponse(code, message, details, Instant.now());
    }
}