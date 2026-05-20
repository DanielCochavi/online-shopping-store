package com.example.onlineshopping.dto;

import com.example.onlineshopping.model.UserRole;
import java.time.Instant;

public record AuthResponse(
        String token,
        String tokenType,
        String userId,
        UserRole role,
        Instant expiresAt) {}
