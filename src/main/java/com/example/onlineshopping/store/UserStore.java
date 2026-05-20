package com.example.onlineshopping.store;

import com.example.onlineshopping.model.User;
import com.example.onlineshopping.exception.ApiException;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class UserStore {

    // Runtime storage for the assignment; putIfAbsent gives atomic duplicate detection without a database.
    private final ConcurrentMap<String, User> users = new ConcurrentHashMap<>();

    public User create(User user) {
        User previous = users.putIfAbsent(user.getId(), user);
        if (previous != null) {
            throw new ApiException(HttpStatus.CONFLICT, "USER_ALREADY_EXISTS",
                    "User already exists: " + user.getId());
        }
        return user;
    }

    public Optional<User> findById(String userId) {
        return Optional.ofNullable(users.get(userId));
    }

    public User requireById(String userId) {
        return findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND",
                        "User not found: " + userId));
    }

    public Collection<User> findAll() {
        return users.values();
    }

    public void clear() {
        users.clear();
    }
}
