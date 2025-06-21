package com.toubson.modulith.catalog.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * ProductVariant entity for representing different variants of a product.
 * For example, a t-shirt might have variants for different sizes and colors.
 */
@Data
@Entity
@Table(name = "product_variants")
public class ProductVariant {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String sku;

    /**
     * Price override for this variant. If null, the product's price is used.
     */
    private BigDecimal price;

    /**
     * The product this variant belongs to
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Product product;

    /**
     * Attributes for this variant (e.g., size, color, etc.)
     */
    @OneToMany(mappedBy = "variant", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<VariantAttribute> attributes = new HashSet<>();

    /**
     * Inventory for this variant
     */
    @OneToOne(mappedBy = "variant", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private VariantInventory inventory;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    private Instant updatedAt;

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    /**
     * Adds an attribute to this variant
     *
     * @param attribute the attribute to add
     * @return this variant for method chaining
     */
    public ProductVariant addAttribute(VariantAttribute attribute) {
        attributes.add(attribute);
        attribute.setVariant(this);
        return this;
    }

    /**
     * Removes an attribute from this variant
     *
     * @param attribute the attribute to remove
     * @return this variant for method chaining
     */
    public ProductVariant removeAttribute(VariantAttribute attribute) {
        attributes.remove(attribute);
        attribute.setVariant(null);
        return this;
    }

    /**
     * Gets the effective price for this variant.
     * If the variant has a price override, that price is used.
     * Otherwise, the product's price is used.
     *
     * @return the effective price for this variant
     */
    @Transient
    public BigDecimal getEffectivePrice() {
        return price != null ? price : product.getPrice();
    }
}