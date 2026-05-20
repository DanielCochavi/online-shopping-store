package com.example.onlineshopping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.onlineshopping.model.ContactType;
import com.example.onlineshopping.model.Product;
import com.example.onlineshopping.model.UserRole;
import com.example.onlineshopping.model.User;
import com.example.onlineshopping.dto.AuthResponse;
import com.example.onlineshopping.exception.ApiException;
import com.example.onlineshopping.service.ProductService;
import com.example.onlineshopping.store.ProductStore;
import com.example.onlineshopping.store.UserStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class OnlineShoppingStoreApplicationTests {

    private static final String JWT_SECRET =
            "dev-only-change-me-in-production-dev-only-change-me-in-production";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserStore userStore;

    @Autowired
    private ProductStore productStore;

    @Autowired
    private ProductService productService;

    @BeforeEach
    void setUp() {
        userStore.clear();
        productStore.clear();

        userStore.create(new User("admin-1", UserRole.ADMIN, ContactType.EMAIL, "admin@example.com"));
        userStore.create(new User("client-1", UserRole.CLIENT, ContactType.EMAIL, "client1@example.com"));
        userStore.create(new User("client-2", UserRole.CLIENT, ContactType.PHONE, "+972501111111"));

        productStore.create(new Product("prod-active", "Active Product", "Available",
                new BigDecimal("15.00"), true, 3));
        productStore.create(new Product("prod-inactive", "Inactive Product", "Unavailable",
                new BigDecimal("20.00"), false, 1));
        productStore.create(new Product("prod-zero", "Zero Quantity", "No stock",
                new BigDecimal("30.00"), true, 0));
    }

    @Test
    void clientCreationSucceedsAndDuplicateClientFails() throws Exception {
        mockMvc.perform(post("/api/users/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"client-3","contactType":"EMAIL","contactValue":"client3@example.com"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("client-3"))
                .andExpect(jsonPath("$.role").value("CLIENT"))
                .andExpect(jsonPath("$.productIds").isEmpty());

        mockMvc.perform(post("/api/users/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"client-3","contactType":"EMAIL","contactValue":"client3@example.com"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("USER_ALREADY_EXISTS"));
    }

    @Test
    void authenticationSucceedsAndFailsForWrongContactOrUnknownUser() throws Exception {
        login("client-1", "EMAIL", "client1@example.com")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("CLIENT"))
                .andExpect(jsonPath("$.token").isNotEmpty());

        login("client-1", "PHONE", "client1@example.com")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"));

        login("unknown", "EMAIL", "nobody@example.com")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"));
    }

    @Test
    void jwtProtectedEndpointsRequireAuthenticationAndCorrectRole() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validProductJson("prod-new")))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/products")
                        .header(HttpHeaders.AUTHORIZATION, bearer(clientToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validProductJson("prod-new")))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/products/prod-active/quantity")
                        .header(HttpHeaders.AUTHORIZATION, bearer(clientToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quantity":5}
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/products/prod-inactive/activate")
                        .header(HttpHeaders.AUTHORIZATION, bearer(clientToken())))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/products")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validProductJson("prod-new")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("prod-new"))
                .andExpect(jsonPath("$.quantity").value(7));
    }

    @Test
    void adminCanUpdateProductsAndQuantity() throws Exception {
        String adminToken = adminToken();

        mockMvc.perform(patch("/api/products/prod-active/description")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Updated description"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Updated description"));

        mockMvc.perform(patch("/api/products/prod-active/price")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"price":99.95}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(99.95));

        mockMvc.perform(patch("/api/products/prod-active/quantity")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quantity":8}
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("prod-active"))
                .andExpect(jsonPath("$.quantity").value(8));

        mockMvc.perform(patch("/api/products/prod-active/deactivate")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(patch("/api/products/prod-active/activate")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("prod-active"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void activeProductsReflectDeactivateAndActivate() throws Exception {
        String adminToken = adminToken();

        mockMvc.perform(get("/api/products/active")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value("prod-active"))
                .andExpect(jsonPath("$[1].id").value("prod-zero"));

        mockMvc.perform(patch("/api/products/prod-active/deactivate")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(get("/api/products/active")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("prod-zero"));

        mockMvc.perform(patch("/api/products/prod-active/activate")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(get("/api/products/active")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value("prod-active"))
                .andExpect(jsonPath("$[1].id").value("prod-zero"));
    }

    @Test
    void myProductsEndpointDoesNotExposeAnotherClientsProducts() throws Exception {
        userStore.requireById("client-2").addProduct("prod-active");

        mockMvc.perform(get("/api/products/my-products")
                        .header(HttpHeaders.AUTHORIZATION, bearer(clientToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void activeProductsEndpointIsAvailableToAdminAndClient() throws Exception {
        mockMvc.perform(get("/api/products/active")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value("prod-active"))
                .andExpect(jsonPath("$[1].id").value("prod-zero"));

        mockMvc.perform(get("/api/products/active")
                        .header(HttpHeaders.AUTHORIZATION, bearer(clientToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value("prod-active"))
                .andExpect(jsonPath("$[1].id").value("prod-zero"));
    }

    @Test
    void adminCannotCallClientOnlyEndpoints() throws Exception {
        mockMvc.perform(get("/api/products/my-products")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isForbidden());
    }

    @Test
    void clientCanViewActiveProductsBuyAndViewOnlyOwnedProducts() throws Exception {
        String clientToken = clientToken();

        mockMvc.perform(get("/api/products/active")
                        .header(HttpHeaders.AUTHORIZATION, bearer(clientToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].active").value(true))
                .andExpect(jsonPath("$[1].active").value(true));

        mockMvc.perform(post("/api/products/prod-active/buy")
                        .header(HttpHeaders.AUTHORIZATION, bearer(clientToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("prod-active"))
                .andExpect(jsonPath("$.quantity").value(2));
        assertThat(productStore.requireById("prod-active").getQuantity()).isEqualTo(2);

        mockMvc.perform(get("/api/products/my-products")
                        .header(HttpHeaders.AUTHORIZATION, bearer(clientToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("prod-active"));
    }

    @Test
    void duplicateOwnershipFailsWithConflict() throws Exception {
        String clientToken = clientToken();

        mockMvc.perform(post("/api/products/prod-active/buy")
                        .header(HttpHeaders.AUTHORIZATION, bearer(clientToken)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/products/prod-active/buy")
                        .header(HttpHeaders.AUTHORIZATION, bearer(clientToken)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("PRODUCT_ALREADY_OWNED"));
    }

    @Test
    void productQuantityEdgeCasesReturnExpectedErrors() throws Exception {
        String clientToken = clientToken();

        mockMvc.perform(post("/api/products/missing/buy")
                        .header(HttpHeaders.AUTHORIZATION, bearer(clientToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("PRODUCT_NOT_FOUND"));

        mockMvc.perform(post("/api/products/prod-inactive/buy")
                        .header(HttpHeaders.AUTHORIZATION, bearer(clientToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("PRODUCT_INACTIVE"));

        mockMvc.perform(post("/api/products/prod-zero/buy")
                        .header(HttpHeaders.AUTHORIZATION, bearer(clientToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("OUT_OF_STOCK"));
    }

    @Test
    void invalidRequestsAreRejected() throws Exception {
        mockMvc.perform(post("/api/users/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"","contactType":"EMAIL","contactValue":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        mockMvc.perform(post("/api/products")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id":"bad","name":"Bad","description":"Bad","price":0,"quantity":1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        mockMvc.perform(post("/api/products")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id":"bad-quantity","name":"Bad","description":"Bad","price":1.00,"quantity":-1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        mockMvc.perform(patch("/api/products/prod-active/quantity")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quantity":-1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void missingInvalidAndExpiredJwtAreRejected() throws Exception {
        mockMvc.perform(get("/api/products/active"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/products/active")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer not-a-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INVALID_TOKEN"));

        mockMvc.perform(get("/api/products/active")
                        .header(HttpHeaders.AUTHORIZATION, bearer(expiredClientToken())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INVALID_TOKEN"));
    }

    @Test
    void updatingMissingProductFails() throws Exception {
        String adminToken = adminToken();

        mockMvc.perform(patch("/api/products/missing/price")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"price":10.00}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("PRODUCT_NOT_FOUND"));

        mockMvc.perform(patch("/api/products/missing/quantity")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quantity":1}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("PRODUCT_NOT_FOUND"));

        mockMvc.perform(patch("/api/products/missing/activate")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("PRODUCT_NOT_FOUND"));
    }

    @Test
    void concurrentDuplicateBuyAttemptsDoNotCreateDuplicateOwnership() throws Exception {
        int attempts = 20;
        ExecutorService executor = Executors.newFixedThreadPool(8);
        try {
            List<Callable<Boolean>> calls = IntStream.range(0, attempts)
                    .mapToObj(i -> (Callable<Boolean>) () -> {
                        try {
                            productService.buyProductForCurrentUser("client-1", "prod-active");
                            return true;
                        } catch (ApiException ex) {
                            return false;
                        }
                    })
                    .toList();

            long successes = executor.invokeAll(calls).stream()
                    .filter(future -> {
                        try {
                            return future.get();
                        } catch (Exception ex) {
                            return false;
                        }
                    })
                    .count();

            assertThat(successes).isEqualTo(1);
            assertThat(userStore.requireById("client-1").getProductIds()).containsExactly("prod-active");
            assertThat(productStore.requireById("prod-active").getQuantity()).isEqualTo(2);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void concurrentAttemptsForLastProductUnitDoNotOversell() throws Exception {
        productStore.create(new Product("prod-last", "Last Item", "Single unit",
                new BigDecimal("50.00"), true, 1));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            List<Callable<Boolean>> calls = List.of(
                    () -> tryBuy("client-1", "prod-last"),
                    () -> tryBuy("client-2", "prod-last"));

            long successes = executor.invokeAll(calls).stream()
                    .filter(future -> {
                        try {
                            return future.get();
                        } catch (Exception ex) {
                            return false;
                        }
                    })
                    .count();

            assertThat(successes).isEqualTo(1);
            assertThat(productStore.requireById("prod-last").getQuantity()).isZero();
        } finally {
            executor.shutdownNow();
        }
    }

    private boolean tryBuy(String clientId, String productId) {
        try {
            productService.buyProductForCurrentUser(clientId, productId);
            return true;
        } catch (ApiException ex) {
            return false;
        }
    }

    private org.springframework.test.web.servlet.ResultActions login(
            String userId, String contactType, String contactValue) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"userId":"%s","contactType":"%s","contactValue":"%s"}
                        """.formatted(userId, contactType, contactValue)));
    }

    private String adminToken() throws Exception {
        return tokenFor("admin-1", "EMAIL", "admin@example.com");
    }

    private String clientToken() throws Exception {
        return tokenFor("client-1", "EMAIL", "client1@example.com");
    }

    private String tokenFor(String userId, String contactType, String contactValue) throws Exception {
        String response = login(userId, contactType, contactValue)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readValue(response, AuthResponse.class).token();
    }

    private String expiredClientToken() {
        SecretKey key = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now();
        return Jwts.builder()
                .subject("client-1")
                .claim("role", "CLIENT")
                .issuedAt(Date.from(now.minusSeconds(3600)))
                .expiration(Date.from(now.minusSeconds(60)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String validProductJson(String productId) {
        return """
                {"id":"%s","name":"New Product","description":"New description","price":12.50,"quantity":7}
                """.formatted(productId);
    }
}
