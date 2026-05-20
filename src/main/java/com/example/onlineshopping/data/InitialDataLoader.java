package com.example.onlineshopping.data;

import com.example.onlineshopping.model.ContactType;
import com.example.onlineshopping.model.Product;
import com.example.onlineshopping.model.UserRole;
import com.example.onlineshopping.model.User;
import com.example.onlineshopping.store.ProductStore;
import com.example.onlineshopping.store.UserStore;
import java.math.BigDecimal;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class InitialDataLoader implements ApplicationRunner {

    private final UserStore userStore;
    private final ProductStore productStore;

    public InitialDataLoader(UserStore userStore, ProductStore productStore) {
        this.userStore = userStore;
        this.productStore = productStore;
    }

    @Override
    public void run(ApplicationArguments args) {
        userStore.create(new User("admin-1", UserRole.ADMIN, ContactType.EMAIL, "admin@example.com"));
        userStore.create(new User("client-1", UserRole.CLIENT, ContactType.EMAIL, "client1@example.com"));
        userStore.create(new User("client-2", UserRole.CLIENT, ContactType.PHONE, "+972501111111"));

        productStore.create(new Product("prod-laptop", "Laptop", "Lightweight business laptop",
                new BigDecimal("1299.99"), true, 5));
        productStore.create(new Product("prod-headphones", "Headphones", "Wireless noise-cancelling headphones",
                new BigDecimal("199.90"), true, 10));
        productStore.create(new Product("prod-monitor", "Monitor", "27 inch 4K monitor",
                new BigDecimal("349.50"), true, 2));
        productStore.create(new Product("prod-legacy", "Legacy Mouse", "Inactive product example",
                new BigDecimal("29.99"), false, 0));
    }
}
