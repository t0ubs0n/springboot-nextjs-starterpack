package com.toubson.modulith.catalog.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Base product entity that can be extended for different product types.
 * Implements soft delete pattern using @SQLDelete and @Where annotations.
 */
@Data
@Entity
@Table(name = "products")
@Inheritance(strategy = InheritanceType.JOINED)
@SQLDelete(sql = "UPDATE products SET active = false WHERE id = ?")
@Where(clause = "active = true")
public class Product {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private String sku;

    @Column(nullable = false)
    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Category category;

    @ManyToMany
    @JoinTable(
            name = "product_tags",
            joinColumns = @JoinColumn(name = "product_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<Tag> tags = new HashSet<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<ProductVariant> variants = new HashSet<>();

    @OneToOne(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Inventory inventory;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductType productType = ProductType.STANDARD;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    private Instant updatedAt;

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    /**
     * Adds a tag to the product
     *
     * @param tag the tag to add
     * @return the product instance for method chaining
     */
    public Product addTag(Tag tag) {
        tags.add(tag);
        return this;
    }

    /**
     * Removes a tag from the product
     *
     * @param tag the tag to remove
     * @return the product instance for method chaining
     */
    public Product removeTag(Tag tag) {
        tags.remove(tag);
        return this;
    }

    /**
     * Adds a variant to the product
     *
     * @param variant the variant to add
     * @return the product instance for method chaining
     */
    public Product addVariant(ProductVariant variant) {
        variants.add(variant);
        variant.setProduct(this);
        return this;
    }

    /**
     * Removes a variant from the product
     *
     * @param variant the variant to remove
     * @return the product instance for method chaining
     */
    public Product removeVariant(ProductVariant variant) {
        variants.remove(variant);
        variant.setProduct(null);
        return this;
    }
}