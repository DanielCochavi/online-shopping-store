package com.example.onlineshopping.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateDescriptionRequest(@NotBlank String description) {
}
