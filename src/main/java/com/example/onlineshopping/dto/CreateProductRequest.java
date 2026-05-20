package com.example.onlineshopping.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CreateProductRequest(
        @NotBlank String id,
        @NotBlank String name,
        @NotBlank String description,
        @NotNull @DecimalMin(value = "0.01") BigDecimal price,
        @Min(0) int quantity
) {
}
