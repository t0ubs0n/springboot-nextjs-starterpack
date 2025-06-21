package com.toubson.modulith.catalog.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Category entity representing a hierarchical category structure.
 * Implements soft delete pattern using @SQLDelete and @Where annotations.
 */
@Data
@Entity
@Table(name = "categories")
@SQLDelete(sql = "UPDATE categories SET active = false WHERE id = ?")
@Where(clause = "active = true")
public class Category {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    private int sortOrder = 0;

    @Column(nullable = false)
    private boolean active = true;

    /**
     * Self-referencing relationship for parent category
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Category parent;

    /**
     * Self-referencing relationship for child categories
     */
    @OneToMany(mappedBy = "parent", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<Category> children = new HashSet<>();

    /**
     * Products in this category
     */
    @OneToMany(mappedBy = "category")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<Product> products = new HashSet<>();

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    private Instant updatedAt;

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    /**
     * Adds a child category to this category
     *
     * @param child the child category to add
     * @return this category for method chaining
     */
    public Category addChild(Category child) {
        children.add(child);
        child.setParent(this);
        return this;
    }

    /**
     * Removes a child category from this category
     *
     * @param child the child category to remove
     * @return this category for method chaining
     */
    public Category removeChild(Category child) {
        children.remove(child);
        child.setParent(null);
        return this;
    }

    /**
     * Checks if this category is a root category (has no parent)
     *
     * @return true if this is a root category, false otherwise
     */
    public boolean isRoot() {
        return parent == null;
    }

    /**
     * Checks if this category is a leaf category (has no children)
     *
     * @return true if this is a leaf category, false otherwise
     */
    public boolean isLeaf() {
        return children.isEmpty();
    }
}