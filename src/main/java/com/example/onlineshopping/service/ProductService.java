package com.example.onlineshopping.service;

import com.example.onlineshopping.model.Product;
import com.example.onlineshopping.model.UserRole;
import com.example.onlineshopping.model.User;
import com.example.onlineshopping.dto.CreateProductRequest;
import com.example.onlineshopping.dto.ProductResponse;
import com.example.onlineshopping.exception.ApiException;
import com.example.onlineshopping.store.ProductStore;
import com.example.onlineshopping.store.UserStore;
import java.util.Comparator;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class ProductService {

    private final ProductStore productStore;
    private final UserStore userStore;
    
    // Simple lock for this small in-memory project.
    // It makes the buy operation safe when two requests happen at the same time.
    // In a real large-scale app, this would be handled by the database or by locking only the specific product.
    private final Object buyLock = new Object();

    public ProductService(ProductStore productStore, UserStore userStore) {
        this.productStore = productStore;
        this.userStore = userStore;
    }

    public ProductResponse createProduct(CreateProductRequest request) {
        Product product = productStore.create(new Product(
                request.id(),
                request.name(),
                request.description(),
                request.price(),
                true,
                request.quantity()));
        return toResponse(product);
    }

    public ProductResponse updateDescription(String productId, String description) {
        Product product = productStore.requireById(productId);
        product.setDescription(description);
        return toResponse(product);
    }

    public ProductResponse updatePrice(String productId, java.math.BigDecimal price) {
        Product product = productStore.requireById(productId);
        product.setPrice(price);
        return toResponse(product);
    }

    public ProductResponse deactivateProduct(String productId) {
        Product product = productStore.requireById(productId);
        product.setActive(false);
        return toResponse(product);
    }

    public ProductResponse activateProduct(String productId) {
        Product product = productStore.requireById(productId);
        product.setActive(true);
        return toResponse(product);
    }

    public ProductResponse updateQuantity(String productId, int quantity) {
        Product product = productStore.requireById(productId);
        product.setQuantity(quantity);
        return toResponse(product);
    }

    public List<ProductResponse> getActiveProducts() {
        return productStore.findAll().stream()
                .filter(Product::isActive)
                .sorted(Comparator.comparing(Product::getId))
                .map(this::toResponse)
                .toList();
    }

    public List<ProductResponse> getProductsForClient(String authenticatedUserId) {
        User user = requireClient(authenticatedUserId);
        return user.getProductIds().stream()
                .map(productStore::requireById)
                .sorted(Comparator.comparing(Product::getId))
                .map(this::toResponse)
                .toList();
    }

    public ProductResponse buyProductForCurrentUser(String authenticatedUserId, String productId) {
        synchronized (buyLock) {
            User user = requireClient(authenticatedUserId);
            Product product = productStore.requireById(productId);
            if (!product.isActive()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "PRODUCT_INACTIVE",
                        "Product is inactive: " + productId);
            }

            if (product.getQuantity() <= 0) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "OUT_OF_STOCK",
                        "Product quantity is zero for product: " + productId);
            }

            if (user.ownsProduct(productId)) {
                throw new ApiException(HttpStatus.CONFLICT, "PRODUCT_ALREADY_OWNED",
                        "Client already owns product: " + productId);
            }

            boolean added = user.addProduct(productId);
            if (!added) {
                throw new ApiException(HttpStatus.CONFLICT, "PRODUCT_ALREADY_OWNED",
                        "Client already owns product: " + productId);
            }
            product.decrementQuantity();

            return toResponse(product);
        }
    }

    private User requireClient(String authenticatedUserId) {
        User user = userStore.requireById(authenticatedUserId);
        if (user.getRole() != UserRole.CLIENT) {
            throw new ApiException(HttpStatus.FORBIDDEN, "CLIENT_ROLE_REQUIRED",
                    "Client role is required");
        }
        return user;
    }

    private ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.isActive(),
                product.getQuantity());
    }
}
