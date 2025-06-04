package com.toubson.modulith.catalog.infrastructure;

import com.toubson.modulith.catalog.domain.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Category entities.
 */
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    /**
     * Find a category by its slug
     *
     * @param slug the slug to search for
     * @return an Optional containing the category if found, empty otherwise
     */
    Optional<Category> findBySlug(String slug);

    /**
     * Find categories by name containing the given text (case insensitive)
     *
     * @param name     the name to search for
     * @param pageable pagination information
     * @return a Page of categories with names containing the given text
     */
    Page<Category> findByNameContainingIgnoreCase(String name, Pageable pageable);

    /**
     * Find all root categories (categories with no parent)
     *
     * @return a list of root categories
     */
    List<Category> findByParentIsNull();

    /**
     * Find all root categories (categories with no parent) with pagination
     *
     * @param pageable pagination information
     * @return a Page of root categories
     */
    Page<Category> findByParentIsNull(Pageable pageable);

    /**
     * Find all child categories of a given parent category
     *
     * @param parent the parent category
     * @return a list of child categories
     */
    List<Category> findByParent(Category parent);

    /**
     * Find all child categories of a given parent category with pagination
     *
     * @param parent   the parent category
     * @param pageable pagination information
     * @return a Page of child categories
     */
    Page<Category> findByParent(Category parent, Pageable pageable);

    /**
     * Find all categories by active status
     *
     * @param active   the active status to search for
     * @param pageable pagination information
     * @return a Page of categories with the given active status
     */
    Page<Category> findByActive(boolean active, Pageable pageable);

    /**
     * Find all categories by parent and active status
     *
     * @param parent   the parent category
     * @param active   the active status to search for
     * @param pageable pagination information
     * @return a Page of categories with the given parent and active status
     */
    Page<Category> findByParentAndActive(Category parent, boolean active, Pageable pageable);

    /**
     * Find all ancestor categories of a given category
     *
     * @param categoryId the ID of the category
     * @return a list of ancestor categories, ordered from the immediate parent to the root
     */
    @Query(value = "WITH RECURSIVE ancestors AS (" +
            "SELECT c.* FROM categories c WHERE c.id = :categoryId " +
            "UNION ALL " +
            "SELECT c.* FROM categories c JOIN ancestors a ON c.id = a.parent_id " +
            "WHERE c.active = true" +
            ") " +
            "SELECT * FROM ancestors WHERE id != :categoryId", nativeQuery = true)
    List<Category> findAncestors(@Param("categoryId") UUID categoryId);

    /**
     * Find all descendant categories of a given category
     *
     * @param categoryId the ID of the category
     * @return a list of descendant categories
     */
    @Query(value = "WITH RECURSIVE descendants AS (" +
            "SELECT c.* FROM categories c WHERE c.id = :categoryId " +
            "UNION ALL " +
            "SELECT c.* FROM categories c JOIN descendants d ON c.parent_id = d.id " +
            "WHERE c.active = true" +
            ") " +
            "SELECT * FROM descendants WHERE id != :categoryId", nativeQuery = true)
    List<Category> findDescendants(@Param("categoryId") UUID categoryId);

    /**
     * Find all sibling categories of a given category (categories with the same parent)
     *
     * @param categoryId the ID of the category
     * @return a list of sibling categories
     */
    @Query("SELECT c FROM Category c WHERE c.parent.id = " +
            "(SELECT c2.parent.id FROM Category c2 WHERE c2.id = :categoryId) " +
            "AND c.id != :categoryId AND c.active = true")
    List<Category> findSiblings(@Param("categoryId") UUID categoryId);

    /**
     * Check if a category with the given slug exists
     *
     * @param slug the slug to check
     * @return true if a category with the given slug exists, false otherwise
     */
    boolean existsBySlug(String slug);
}