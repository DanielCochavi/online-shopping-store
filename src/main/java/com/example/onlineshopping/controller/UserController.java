package com.example.onlineshopping.controller;

import com.example.onlineshopping.dto.CreateClientRequest;
import com.example.onlineshopping.dto.UserResponse;
import com.example.onlineshopping.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/clients")
    @Operation(summary = "Create a CLIENT user in memory. ADMIN users are initialized at application startup.")
    public ResponseEntity<UserResponse> createClient(@Valid @RequestBody CreateClientRequest request) {
        UserResponse response = userService.createClient(request);
        return ResponseEntity.created(URI.create("/api/users/clients/" + response.id())).body(response);
    }
}
