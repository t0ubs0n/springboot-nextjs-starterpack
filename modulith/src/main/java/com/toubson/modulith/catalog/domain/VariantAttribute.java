package com.toubson.modulith.catalog.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.Instant;
import java.util.UUID;

/**
 * VariantAttribute entity for representing attributes of product variants.
 * For example, a t-shirt variant might have attributes like "size" and "color".
 */
@Data
@Entity
@Table(name = "variant_attributes")
public class VariantAttribute {

    @Id
    private UUID id = UUID.randomUUID();

    /**
     * The name of the attribute (e.g., "size", "color")
     */
    @Column(nullable = false)
    private String name;

    /**
     * The value of the attribute (e.g., "large", "red")
     */
    @Column(nullable = false)
    private String value;

    /**
     * The variant this attribute belongs to
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private ProductVariant variant;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    private Instant updatedAt;

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}