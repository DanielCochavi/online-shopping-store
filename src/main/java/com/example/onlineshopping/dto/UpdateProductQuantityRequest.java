package com.example.onlineshopping.dto;

import jakarta.validation.constraints.Min;

public record UpdateProductQuantityRequest(@Min(0) int quantity) {
}
