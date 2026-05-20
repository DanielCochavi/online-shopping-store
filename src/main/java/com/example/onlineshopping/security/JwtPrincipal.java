package com.example.onlineshopping.security;

import com.example.onlineshopping.model.UserRole;

public record JwtPrincipal(String userId, UserRole role) {
}
