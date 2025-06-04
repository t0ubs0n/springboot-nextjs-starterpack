package com.toubson.modulith.catalog.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * TagCategory entity for organizing tags into categories.
 */
@Data
@Entity
@Table(name = "tag_categories")
public class TagCategory {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(nullable = false, unique = true)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, unique = true)
    private String slug;

    /**
     * Tags in this category
     */
    @OneToMany(mappedBy = "category", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<Tag> tags = new HashSet<>();

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    private Instant updatedAt;

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    /**
     * Adds a tag to this category
     *
     * @param tag the tag to add
     * @return this category for method chaining
     */
    public TagCategory addTag(Tag tag) {
        tags.add(tag);
        tag.setCategory(this);
        return this;
    }

    /**
     * Removes a tag from this category
     *
     * @param tag the tag to remove
     * @return this category for method chaining
     */
    public TagCategory removeTag(Tag tag) {
        tags.remove(tag);
        tag.setCategory(null);
        return this;
    }
}