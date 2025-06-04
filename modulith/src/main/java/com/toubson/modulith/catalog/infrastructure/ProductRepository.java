package com.toubson.modulith.catalog.infrastructure;

import com.toubson.modulith.catalog.domain.Category;
import com.toubson.modulith.catalog.domain.Product;
import com.toubson.modulith.catalog.domain.ProductType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Product entities.
 * Extends JpaSpecificationExecutor to support complex queries with specifications.
 */
public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {

    /**
     * Find a product by its SKU
     *
     * @param sku the SKU to search for
     * @return an Optional containing the product if found, empty otherwise
     */
    Optional<Product> findBySku(String sku);

    /**
     * Find products by category
     *
     * @param category the category to search for
     * @param pageable pagination information
     * @return a Page of products in the given category
     */
    Page<Product> findByCategory(Category category, Pageable pageable);

    /**
     * Find products by product type
     *
     * @param productType the product type to search for
     * @param pageable    pagination information
     * @return a Page of products of the given type
     */
    Page<Product> findByProductType(ProductType productType, Pageable pageable);

    /**
     * Find products by name containing the given text (case insensitive)
     *
     * @param name     the name to search for
     * @param pageable pagination information
     * @return a Page of products with names containing the given text
     */
    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);

    /**
     * Find products by price range
     *
     * @param minPrice the minimum price
     * @param maxPrice the maximum price
     * @param pageable pagination information
     * @return a Page of products within the given price range
     */
    Page<Product> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

    /**
     * Find products by category and price range
     *
     * @param category the category to search for
     * @param minPrice the minimum price
     * @param maxPrice the maximum price
     * @param pageable pagination information
     * @return a Page of products in the given category and within the given price range
     */
    Page<Product> findByCategoryAndPriceBetween(Category category, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

    /**
     * Find products by tag name
     *
     * @param tagName  the tag name to search for
     * @param pageable pagination information
     * @return a Page of products with the given tag
     */
    @Query("SELECT p FROM Product p JOIN p.tags t WHERE t.name = :tagName")
    Page<Product> findByTagName(@Param("tagName") String tagName, Pageable pageable);

    /**
     * Find products by tag names (any of the given tags)
     *
     * @param tagNames the tag names to search for
     * @param pageable pagination information
     * @return a Page of products with any of the given tags
     */
    @Query("SELECT DISTINCT p FROM Product p JOIN p.tags t WHERE t.name IN :tagNames")
    Page<Product> findByTagNames(@Param("tagNames") List<String> tagNames, Pageable pageable);

    /**
     * Find products by tag names (all of the given tags)
     *
     * @param tagNames the tag names to search for
     * @param pageable pagination information
     * @return a Page of products with all of the given tags
     */
    @Query("SELECT p FROM Product p JOIN p.tags t WHERE t.name IN :tagNames GROUP BY p HAVING COUNT(DISTINCT t.name) = :tagCount")
    Page<Product> findByAllTagNames(@Param("tagNames") List<String> tagNames, @Param("tagCount") Long tagCount, Pageable pageable);

    /**
     * Full-text search for products by name or description
     *
     * @param searchTerm the search term
     * @param pageable   pagination information
     * @return a Page of products matching the search term
     */
    @Query("SELECT p FROM Product p WHERE " +
            "LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(p.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<Product> search(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Check if a product with the given SKU exists
     *
     * @param sku the SKU to check
     * @return true if a product with the given SKU exists, false otherwise
     */
    boolean existsBySku(String sku);
}