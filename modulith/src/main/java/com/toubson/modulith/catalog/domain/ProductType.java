package com.toubson.modulith.catalog.domain;

import lombok.Getter;

/**
 * Enum representing different types of products.
 * This allows for polymorphic behavior while maintaining a unified API.
 */
@Getter
public enum ProductType {
    STANDARD("Standard product"),
    DIGITAL("Digital product"),
    SERVICE("Service product"),
    SUBSCRIPTION("Subscription product"),
    BUNDLE("Product bundle");

    private final String description;

    ProductType(String description) {
        this.description = description;
    }

}