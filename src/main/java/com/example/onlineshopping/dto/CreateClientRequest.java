package com.example.onlineshopping.dto;

import com.example.onlineshopping.model.ContactType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateClientRequest(
        @NotBlank String userId,
        @NotNull ContactType contactType,
        @NotBlank String contactValue
) {
}
