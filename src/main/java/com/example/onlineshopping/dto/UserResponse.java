package com.example.onlineshopping.dto;

import com.example.onlineshopping.model.ContactType;
import com.example.onlineshopping.model.UserRole;
import java.util.Set;

public record UserResponse(
        String id,
        UserRole role,
        ContactType contactType,
        String contactValue,
        Set<String> productIds
) {
}
