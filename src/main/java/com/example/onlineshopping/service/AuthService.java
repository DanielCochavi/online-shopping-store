package com.example.onlineshopping.service;

import com.example.onlineshopping.model.User;
import com.example.onlineshopping.dto.AuthResponse;
import com.example.onlineshopping.dto.LoginRequest;
import com.example.onlineshopping.exception.ApiException;
import com.example.onlineshopping.security.JwtService;
import com.example.onlineshopping.store.UserStore;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserStore userStore;
    private final JwtService jwtService;

    public AuthService(UserStore userStore, JwtService jwtService) {
        this.userStore = userStore;
        this.jwtService = jwtService;
    }

    public AuthResponse login(LoginRequest request) {
        User user = userStore.findById(request.userId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS",
                        "Invalid user ID or contact method"));

        boolean matches = user.getContactType() == request.contactType()
                && user.getContactValue().equals(request.contactValue());
        if (!matches) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS",
                    "Invalid user ID or contact method");
        }

        return jwtService.generateToken(user);
    }
}
