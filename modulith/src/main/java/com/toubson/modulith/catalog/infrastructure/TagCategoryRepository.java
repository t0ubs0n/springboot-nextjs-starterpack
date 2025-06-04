package com.toubson.modulith.catalog.infrastructure;

import com.toubson.modulith.catalog.domain.TagCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for TagCategory entities.
 */
public interface TagCategoryRepository extends JpaRepository<TagCategory, UUID> {

    /**
     * Find a tag category by its slug
     *
     * @param slug the slug to search for
     * @return an Optional containing the tag category if found, empty otherwise
     */
    Optional<TagCategory> findBySlug(String slug);

    /**
     * Find a tag category by its name
     *
     * @param name the name to search for
     * @return an Optional containing the tag category if found, empty otherwise
     */
    Optional<TagCategory> findByName(String name);

    /**
     * Find tag categories by name containing the given text (case insensitive)
     *
     * @param name     the name to search for
     * @param pageable pagination information
     * @return a Page of tag categories with names containing the given text
     */
    Page<TagCategory> findByNameContainingIgnoreCase(String name, Pageable pageable);

    /**
     * Find tag categories with the most tags
     *
     * @param pageable pagination information
     * @return a list of tag categories with the most tags
     */
    @Query("SELECT tc FROM TagCategory tc LEFT JOIN tc.tags t GROUP BY tc ORDER BY COUNT(t) DESC")
    List<TagCategory> findPopularCategories(Pageable pageable);

    /**
     * Find tag categories that have at least one tag
     *
     * @param pageable pagination information
     * @return a Page of tag categories that have at least one tag
     */
    @Query("SELECT DISTINCT tc FROM TagCategory tc JOIN tc.tags t")
    Page<TagCategory> findCategoriesWithTags(Pageable pageable);

    /**
     * Check if a tag category with the given slug exists
     *
     * @param slug the slug to check
     * @return true if a tag category with the given slug exists, false otherwise
     */
    boolean existsBySlug(String slug);

    /**
     * Check if a tag category with the given name exists
     *
     * @param name the name to check
     * @return true if a tag category with the given name exists, false otherwise
     */
    boolean existsByName(String name);
}