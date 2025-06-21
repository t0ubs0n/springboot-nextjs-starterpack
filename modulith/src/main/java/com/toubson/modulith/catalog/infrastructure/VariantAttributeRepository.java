package com.toubson.modulith.catalog.infrastructure;

import com.toubson.modulith.catalog.domain.ProductVariant;
import com.toubson.modulith.catalog.domain.VariantAttribute;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for VariantAttribute entities.
 */
public interface VariantAttributeRepository extends JpaRepository<VariantAttribute, UUID> {

    /**
     * Find variant attributes by variant
     *
     * @param variant the variant to search for
     * @return a list of variant attributes for the given variant
     */
    List<VariantAttribute> findByVariant(ProductVariant variant);

    /**
     * Find variant attributes by variant with pagination
     *
     * @param variant  the variant to search for
     * @param pageable pagination information
     * @return a Page of variant attributes for the given variant
     */
    Page<VariantAttribute> findByVariant(ProductVariant variant, Pageable pageable);

    /**
     * Find variant attributes by name
     *
     * @param name     the name to search for
     * @param pageable pagination information
     * @return a Page of variant attributes with the given name
     */
    Page<VariantAttribute> findByName(String name, Pageable pageable);

    /**
     * Find variant attributes by value
     *
     * @param value    the value to search for
     * @param pageable pagination information
     * @return a Page of variant attributes with the given value
     */
    Page<VariantAttribute> findByValue(String value, Pageable pageable);

    /**
     * Find variant attributes by name and value
     *
     * @param name     the name to search for
     * @param value    the value to search for
     * @param pageable pagination information
     * @return a Page of variant attributes with the given name and value
     */
    Page<VariantAttribute> findByNameAndValue(String name, String value, Pageable pageable);

    /**
     * Find variant attributes by variant and name
     *
     * @param variant the variant to search for
     * @param name    the name to search for
     * @return a list of variant attributes for the given variant and with the given name
     */
    List<VariantAttribute> findByVariantAndName(ProductVariant variant, String name);

    /**
     * Find variant attributes by variant and name and value
     *
     * @param variant the variant to search for
     * @param name    the name to search for
     * @param value   the value to search for
     * @return an Optional containing the variant attribute if found, empty otherwise
     */
    Optional<VariantAttribute> findByVariantAndNameAndValue(ProductVariant variant, String name, String value);

    /**
     * Find distinct attribute names
     *
     * @return a list of distinct attribute names
     */
    @Query("SELECT DISTINCT a.name FROM VariantAttribute a ORDER BY a.name")
    List<String> findDistinctNames();

    /**
     * Find distinct attribute values for a given name
     *
     * @param name the name to search for
     * @return a list of distinct attribute values for the given name
     */
    @Query("SELECT DISTINCT a.value FROM VariantAttribute a WHERE a.name = :name ORDER BY a.value")
    List<String> findDistinctValuesByName(@Param("name") String name);

    /**
     * Delete all attributes for a given variant
     *
     * @param variant the variant to delete attributes for
     */
    void deleteByVariant(ProductVariant variant);
}