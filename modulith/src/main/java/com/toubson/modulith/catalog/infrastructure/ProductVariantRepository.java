package com.toubson.modulith.catalog.infrastructure;

import com.toubson.modulith.catalog.domain.Product;
import com.toubson.modulith.catalog.domain.ProductVariant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for ProductVariant entities.
 */
public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID> {

    /**
     * Find a product variant by its SKU
     *
     * @param sku the SKU to search for
     * @return an Optional containing the product variant if found, empty otherwise
     */
    Optional<ProductVariant> findBySku(String sku);

    /**
     * Find product variants by product
     *
     * @param product the product to search for
     * @return a list of product variants for the given product
     */
    List<ProductVariant> findByProduct(Product product);

    /**
     * Find product variants by product with pagination
     *
     * @param product  the product to search for
     * @param pageable pagination information
     * @return a Page of product variants for the given product
     */
    Page<ProductVariant> findByProduct(Product product, Pageable pageable);

    /**
     * Find product variants by name containing the given text (case insensitive)
     *
     * @param name     the name to search for
     * @param pageable pagination information
     * @return a Page of product variants with names containing the given text
     */
    Page<ProductVariant> findByNameContainingIgnoreCase(String name, Pageable pageable);

    /**
     * Find product variants by price range
     *
     * @param minPrice the minimum price
     * @param maxPrice the maximum price
     * @param pageable pagination information
     * @return a Page of product variants within the given price range
     */
    Page<ProductVariant> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

    /**
     * Find product variants by product and price range
     *
     * @param product  the product to search for
     * @param minPrice the minimum price
     * @param maxPrice the maximum price
     * @param pageable pagination information
     * @return a Page of product variants for the given product and within the given price range
     */
    Page<ProductVariant> findByProductAndPriceBetween(Product product, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

    /**
     * Find product variants by attribute name and value
     *
     * @param attributeName  the attribute name to search for
     * @param attributeValue the attribute value to search for
     * @param pageable       pagination information
     * @return a Page of product variants with the given attribute name and value
     */
    @Query("SELECT DISTINCT pv FROM ProductVariant pv JOIN pv.attributes a WHERE a.name = :attributeName AND a.value = :attributeValue")
    Page<ProductVariant> findByAttributeNameAndValue(@Param("attributeName") String attributeName, @Param("attributeValue") String attributeValue, Pageable pageable);

    /**
     * Find product variants by product and attribute name and value
     *
     * @param product        the product to search for
     * @param attributeName  the attribute name to search for
     * @param attributeValue the attribute value to search for
     * @param pageable       pagination information
     * @return a Page of product variants for the given product and with the given attribute name and value
     */
    @Query("SELECT DISTINCT pv FROM ProductVariant pv JOIN pv.attributes a WHERE pv.product = :product AND a.name = :attributeName AND a.value = :attributeValue")
    Page<ProductVariant> findByProductAndAttributeNameAndValue(@Param("product") Product product, @Param("attributeName") String attributeName, @Param("attributeValue") String attributeValue, Pageable pageable);

    /**
     * Check if a product variant with the given SKU exists
     *
     * @param sku the SKU to check
     * @return true if a product variant with the given SKU exists, false otherwise
     */
    boolean existsBySku(String sku);
}