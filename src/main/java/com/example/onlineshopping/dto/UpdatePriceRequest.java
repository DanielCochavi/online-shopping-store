package com.example.onlineshopping.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record UpdatePriceRequest(@NotNull @DecimalMin(value = "0.01") BigDecimal price) {
}
