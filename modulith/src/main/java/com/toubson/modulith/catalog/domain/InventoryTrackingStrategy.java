package com.toubson.modulith.catalog.domain;

/**
 * Enum representing different inventory tracking strategies.
 */
public enum InventoryTrackingStrategy {
    /**
     * Track inventory with finite stock levels
     */
    FINITE("Track finite stock levels"),

    /**
     * Infinite stock (always available)
     */
    INFINITE("Infinite stock (always available)"),

    /**
     * Do not track inventory
     */
    NOT_TRACKED("Inventory not tracked");

    private final String description;

    InventoryTrackingStrategy(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}