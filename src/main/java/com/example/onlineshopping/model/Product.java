package com.example.onlineshopping.model;

import java.math.BigDecimal;

public class Product {

    private final String id;
    private String name;
    private String description;
    private BigDecimal price;
    private boolean active;
    private int quantity;

    public Product(String id, String name, String description, BigDecimal price, boolean active, int quantity) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.active = active;
        this.quantity = quantity;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public void decrementQuantity() {
        this.quantity--;
    }
}
