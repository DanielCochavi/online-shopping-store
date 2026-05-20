package com.example.onlineshopping.controller;

import com.example.onlineshopping.dto.CreateProductRequest;
import com.example.onlineshopping.dto.ProductResponse;
import com.example.onlineshopping.dto.UpdateDescriptionRequest;
import com.example.onlineshopping.dto.UpdatePriceRequest;
import com.example.onlineshopping.dto.UpdateProductQuantityRequest;
import com.example.onlineshopping.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Products")
@SecurityRequirement(name = "bearerAuth")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a Product with an available quantity")
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody CreateProductRequest request) {
        ProductResponse response = productService.createProduct(request);
        return ResponseEntity.created(URI.create("/api/products/" + response.id())).body(response);
    }

    @PatchMapping("/{productId}/description")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a Product description")
    public ResponseEntity<ProductResponse> updateDescription(
            @PathVariable String productId,
            @Valid @RequestBody UpdateDescriptionRequest request) {
        return ResponseEntity.ok(productService.updateDescription(productId, request.description()));
    }

    @PatchMapping("/{productId}/price")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a Product price")
    public ResponseEntity<ProductResponse> updatePrice(
            @PathVariable String productId,
            @Valid @RequestBody UpdatePriceRequest request) {
        return ResponseEntity.ok(productService.updatePrice(productId, request.price()));
    }

    @PatchMapping("/{productId}/quantity")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a Product quantity")
    public ResponseEntity<ProductResponse> updateQuantity(
            @PathVariable String productId,
            @Valid @RequestBody UpdateProductQuantityRequest request) {
        return ResponseEntity.ok(productService.updateQuantity(productId, request.quantity()));
    }

    @PatchMapping("/{productId}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deactivate a Product")
    public ResponseEntity<ProductResponse> deactivate(@PathVariable String productId) {
        return ResponseEntity.ok(productService.deactivateProduct(productId));
    }

    @PatchMapping("/{productId}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Activate a Product")
    public ResponseEntity<ProductResponse> activate(@PathVariable String productId) {
        return ResponseEntity.ok(productService.activateProduct(productId));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN')")
    @Operation(summary = "Get all active Products")
    public ResponseEntity<List<ProductResponse>> getActiveProducts() {
        return ResponseEntity.ok(productService.getActiveProducts());
    }

    @GetMapping("/my-products")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Get Products assigned to the authenticated CLIENT")
    public ResponseEntity<List<ProductResponse>> getMyProducts(
            @AuthenticationPrincipal String authenticatedUserId) {
        return ResponseEntity.ok(productService.getProductsForClient(authenticatedUserId));
    }

    @PostMapping("/{productId}/buy")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Buy a Product for the authenticated CLIENT")
    public ResponseEntity<ProductResponse> buyProduct(
            @AuthenticationPrincipal String authenticatedUserId,
            @PathVariable String productId) {
        return ResponseEntity.ok(productService.buyProductForCurrentUser(authenticatedUserId, productId));
    }
}
