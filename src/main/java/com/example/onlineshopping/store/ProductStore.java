package com.example.onlineshopping.store;

import com.example.onlineshopping.model.Product;
import com.example.onlineshopping.exception.ApiException;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class ProductStore {

    // Runtime storage for the assignment; putIfAbsent gives atomic duplicate detection without a database.
    private final ConcurrentMap<String, Product> products = new ConcurrentHashMap<>();

    public Product create(Product product) {
        Product previous = products.putIfAbsent(product.getId(), product);
        if (previous != null) {
            throw new ApiException(HttpStatus.CONFLICT, "PRODUCT_ALREADY_EXISTS",
                    "Product already exists: " + product.getId());
        }
        return product;
    }

    public Optional<Product> findById(String productId) {
        return Optional.ofNullable(products.get(productId));
    }

    public Product requireById(String productId) {
        return findById(productId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND",
                        "Product not found: " + productId));
    }

    public Collection<Product> findAll() {
        return products.values();
    }

    public void clear() {
        products.clear();
    }
}
