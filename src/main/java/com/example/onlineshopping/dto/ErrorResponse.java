package com.example.onlineshopping.dto;

import java.time.Instant;

public record ErrorResponse(
        Instant timestamp,
        int status,
        String errorCode,
        String message
) {
}
