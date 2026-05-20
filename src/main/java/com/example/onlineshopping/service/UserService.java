package com.example.onlineshopping.service;

import com.example.onlineshopping.model.UserRole;
import com.example.onlineshopping.model.User;
import com.example.onlineshopping.dto.CreateClientRequest;
import com.example.onlineshopping.dto.UserResponse;
import com.example.onlineshopping.store.UserStore;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserStore userStore;

    public UserService(UserStore userStore) {
        this.userStore = userStore;
    }

    public UserResponse createClient(CreateClientRequest request) {
        User user = userStore.create(new User(
                request.userId(),
                UserRole.CLIENT,
                request.contactType(),
                request.contactValue()));
        return toResponse(user);
    }

    public UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getRole(),
                user.getContactType(),
                user.getContactValue(),
                Set.copyOf(user.getProductIds()));
    }
}
