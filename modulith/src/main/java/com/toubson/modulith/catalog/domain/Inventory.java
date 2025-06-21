package com.toubson.modulith.catalog.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.Instant;
import java.util.UUID;

/**
 * Inventory entity for tracking product stock levels.
 * Uses optimistic locking for concurrent safety.
 */
@Data
@Entity
@Table(name = "inventory")
public class Inventory {

    @Id
    private UUID id = UUID.randomUUID();

    /**
     * The product this inventory belongs to
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false, unique = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Product product;

    /**
     * Current stock quantity
     */
    private Integer stockQuantity;

    /**
     * Low stock threshold for alerts
     */
    private Integer lowStockThreshold;

    /**
     * Reserved stock quantity (for pending orders)
     */
    private Integer reservedQuantity = 0;

    /**
     * Inventory tracking strategy
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InventoryTrackingStrategy trackingStrategy = InventoryTrackingStrategy.FINITE;

    /**
     * Version for optimistic locking
     */
    @Version
    private Long version;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    private Instant updatedAt;

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    /**
     * Checks if the product is in stock
     *
     * @return true if the product is in stock, false otherwise
     */
    @Transient
    public boolean isInStock() {
        if (trackingStrategy == InventoryTrackingStrategy.NOT_TRACKED) {
            return true;
        }
        if (trackingStrategy == InventoryTrackingStrategy.INFINITE) {
            return true;
        }
        return stockQuantity != null && stockQuantity > 0 && stockQuantity > reservedQuantity;
    }

    /**
     * Checks if the product is low in stock
     *
     * @return true if the product is low in stock, false otherwise
     */
    @Transient
    public boolean isLowStock() {
        if (trackingStrategy != InventoryTrackingStrategy.FINITE) {
            return false;
        }
        return stockQuantity != null && lowStockThreshold != null &&
                stockQuantity <= lowStockThreshold;
    }

    /**
     * Gets the available stock quantity (total - reserved)
     *
     * @return the available stock quantity
     */
    @Transient
    public Integer getAvailableQuantity() {
        if (trackingStrategy == InventoryTrackingStrategy.NOT_TRACKED) {
            return null;
        }
        if (trackingStrategy == InventoryTrackingStrategy.INFINITE) {
            return Integer.MAX_VALUE;
        }
        return stockQuantity != null ? stockQuantity - reservedQuantity : 0;
    }

    /**
     * Reserves a quantity of stock
     *
     * @param quantity the quantity to reserve
     * @return true if the reservation was successful, false otherwise
     */
    public boolean reserveStock(int quantity) {
        if (trackingStrategy == InventoryTrackingStrategy.NOT_TRACKED) {
            return true;
        }
        if (trackingStrategy == InventoryTrackingStrategy.INFINITE) {
            return true;
        }
        if (stockQuantity != null && stockQuantity >= quantity + reservedQuantity) {
            reservedQuantity += quantity;
            return true;
        }
        return false;
    }

    /**
     * Releases a quantity of reserved stock
     *
     * @param quantity the quantity to release
     */
    public void releaseStock(int quantity) {
        if (trackingStrategy == InventoryTrackingStrategy.FINITE) {
            reservedQuantity = Math.max(0, reservedQuantity - quantity);
        }
    }

    /**
     * Decreases the stock quantity
     *
     * @param quantity the quantity to decrease
     * @return true if the stock was decreased successfully, false otherwise
     */
    public boolean decreaseStock(int quantity) {
        if (trackingStrategy == InventoryTrackingStrategy.NOT_TRACKED) {
            return true;
        }
        if (trackingStrategy == InventoryTrackingStrategy.INFINITE) {
            return true;
        }
        if (stockQuantity != null && stockQuantity >= quantity) {
            stockQuantity -= quantity;
            reservedQuantity = Math.max(0, reservedQuantity - quantity);
            return true;
        }
        return false;
    }

    /**
     * Increases the stock quantity
     *
     * @param quantity the quantity to increase
     */
    public void increaseStock(int quantity) {
        if (trackingStrategy == InventoryTrackingStrategy.FINITE) {
            if (stockQuantity == null) {
                stockQuantity = 0;
            }
            stockQuantity += quantity;
        }
    }
}