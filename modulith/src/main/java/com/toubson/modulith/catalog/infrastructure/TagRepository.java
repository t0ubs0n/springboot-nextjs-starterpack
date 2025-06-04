package com.toubson.modulith.catalog.infrastructure;

import com.toubson.modulith.catalog.domain.Tag;
import com.toubson.modulith.catalog.domain.TagCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Tag entities.
 */
public interface TagRepository extends JpaRepository<Tag, UUID> {

    /**
     * Find a tag by its slug
     *
     * @param slug the slug to search for
     * @return an Optional containing the tag if found, empty otherwise
     */
    Optional<Tag> findBySlug(String slug);

    /**
     * Find a tag by its name
     *
     * @param name the name to search for
     * @return an Optional containing the tag if found, empty otherwise
     */
    Optional<Tag> findByName(String name);

    /**
     * Find tags by name containing the given text (case insensitive)
     *
     * @param name     the name to search for
     * @param pageable pagination information
     * @return a Page of tags with names containing the given text
     */
    Page<Tag> findByNameContainingIgnoreCase(String name, Pageable pageable);

    /**
     * Find tags by category
     *
     * @param category the category to search for
     * @param pageable pagination information
     * @return a Page of tags in the given category
     */
    Page<Tag> findByCategory(TagCategory category, Pageable pageable);

    /**
     * Find tags by category is null (tags without a category)
     *
     * @param pageable pagination information
     * @return a Page of tags without a category
     */
    Page<Tag> findByCategoryIsNull(Pageable pageable);

    /**
     * Find tags by product ID
     *
     * @param productId the product ID to search for
     * @return a list of tags associated with the given product
     */
    @Query("SELECT t FROM Tag t JOIN t.products p WHERE p.id = :productId")
    List<Tag> findByProductId(@Param("productId") UUID productId);

    /**
     * Find popular tags (tags with the most products)
     *
     * @param pageable pagination information
     * @return a list of popular tags
     */
    @Query("SELECT t FROM Tag t JOIN t.products p GROUP BY t ORDER BY COUNT(p) DESC")
    List<Tag> findPopularTags(Pageable pageable);

    /**
     * Check if a tag with the given slug exists
     *
     * @param slug the slug to check
     * @return true if a tag with the given slug exists, false otherwise
     */
    boolean existsBySlug(String slug);

    /**
     * Check if a tag with the given name exists
     *
     * @param name the name to check
     * @return true if a tag with the given name exists, false otherwise
     */
    boolean existsByName(String name);
}
