package com.example.onlineshopping.model;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class User {

    private final String id;
    private final UserRole role;
    private final ContactType contactType;
    private final String contactValue;
    // Product ownership is a set because a client can be associated with each product at most once.
    private final Set<String> productIds;

    public User(String id, UserRole role, ContactType contactType, String contactValue) {
        this(id, role, contactType, contactValue, ConcurrentHashMap.newKeySet());
    }

    public User(String id, UserRole role, ContactType contactType, String contactValue, Set<String> productIds) {
        this.id = id;
        this.role = role;
        this.contactType = contactType;
        this.contactValue = contactValue;
        this.productIds = ConcurrentHashMap.newKeySet();
        this.productIds.addAll(productIds);
    }

    public String getId() {
        return id;
    }

    public UserRole getRole() {
        return role;
    }

    public ContactType getContactType() {
        return contactType;
    }

    public String getContactValue() {
        return contactValue;
    }

    public Set<String> getProductIds() {
        return Collections.unmodifiableSet(productIds);
    }

    public boolean ownsProduct(String productId) {
        return productIds.contains(productId);
    }

    public boolean addProduct(String productId) {
        return productIds.add(productId);
    }
}
