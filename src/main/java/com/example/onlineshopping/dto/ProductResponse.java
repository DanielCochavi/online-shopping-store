package com.example.onlineshopping.dto;

import java.math.BigDecimal;

public record ProductResponse(
        String id,
        String name,
        String description,
        BigDecimal price,
        boolean active,
        int quantity
) {
}
