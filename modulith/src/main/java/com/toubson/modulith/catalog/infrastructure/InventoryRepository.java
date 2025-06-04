package com.toubson.modulith.catalog.infrastructure;

import com.toubson.modulith.catalog.domain.Inventory;
import com.toubson.modulith.catalog.domain.InventoryTrackingStrategy;
import com.toubson.modulith.catalog.domain.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Inventory entities.
 */
public interface InventoryRepository extends JpaRepository<Inventory, UUID> {

    /**
     * Find inventory by product
     *
     * @param product the product to search for
     * @return an Optional containing the inventory if found, empty otherwise
     */
    Optional<Inventory> findByProduct(Product product);

    /**
     * Find inventory by product with pessimistic lock
     *
     * @param product the product to search for
     * @return an Optional containing the inventory if found, empty otherwise
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.product = :product")
    Optional<Inventory> findByProductWithLock(@Param("product") Product product);

    /**
     * Find inventory by product ID
     *
     * @param productId the product ID to search for
     * @return an Optional containing the inventory if found, empty otherwise
     */
    @Query("SELECT i FROM Inventory i WHERE i.product.id = :productId")
    Optional<Inventory> findByProductId(@Param("productId") UUID productId);

    /**
     * Find inventory by product ID with pessimistic lock
     *
     * @param productId the product ID to search for
     * @return an Optional containing the inventory if found, empty otherwise
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.product.id = :productId")
    Optional<Inventory> findByProductIdWithLock(@Param("productId") UUID productId);

    /**
     * Find inventory by tracking strategy
     *
     * @param trackingStrategy the tracking strategy to search for
     * @param pageable         pagination information
     * @return a Page of inventory with the given tracking strategy
     */
    Page<Inventory> findByTrackingStrategy(InventoryTrackingStrategy trackingStrategy, Pageable pageable);

    /**
     * Find inventory with stock quantity less than or equal to low stock threshold
     *
     * @param pageable pagination information
     * @return a Page of inventory with stock quantity less than or equal to low stock threshold
     */
    @Query("SELECT i FROM Inventory i WHERE i.trackingStrategy = 'FINITE' AND i.stockQuantity <= i.lowStockThreshold")
    Page<Inventory> findLowStock(Pageable pageable);

    /**
     * Find inventory with stock quantity less than the given threshold
     *
     * @param threshold the threshold to search for
     * @param pageable  pagination information
     * @return a Page of inventory with stock quantity less than the given threshold
     */
    @Query("SELECT i FROM Inventory i WHERE i.trackingStrategy = 'FINITE' AND i.stockQuantity < :threshold")
    Page<Inventory> findByStockQuantityLessThan(@Param("threshold") Integer threshold, Pageable pageable);

    /**
     * Find inventory with stock quantity greater than the given threshold
     *
     * @param threshold the threshold to search for
     * @param pageable  pagination information
     * @return a Page of inventory with stock quantity greater than the given threshold
     */
    @Query("SELECT i FROM Inventory i WHERE i.trackingStrategy = 'FINITE' AND i.stockQuantity > :threshold")
    Page<Inventory> findByStockQuantityGreaterThan(@Param("threshold") Integer threshold, Pageable pageable);

    /**
     * Find inventory with stock quantity between the given thresholds
     *
     * @param minThreshold the minimum threshold to search for
     * @param maxThreshold the maximum threshold to search for
     * @param pageable     pagination information
     * @return a Page of inventory with stock quantity between the given thresholds
     */
    @Query("SELECT i FROM Inventory i WHERE i.trackingStrategy = 'FINITE' AND i.stockQuantity BETWEEN :minThreshold AND :maxThreshold")
    Page<Inventory> findByStockQuantityBetween(@Param("minThreshold") Integer minThreshold, @Param("maxThreshold") Integer maxThreshold, Pageable pageable);

    /**
     * Find inventory with reserved quantity greater than zero
     *
     * @param pageable pagination information
     * @return a Page of inventory with reserved quantity greater than zero
     */
    @Query("SELECT i FROM Inventory i WHERE i.reservedQuantity > 0")
    Page<Inventory> findWithReservedStock(Pageable pageable);

    /**
     * Find out of stock inventory
     *
     * @param pageable pagination information
     * @return a Page of out of stock inventory
     */
    @Query("SELECT i FROM Inventory i WHERE i.trackingStrategy = 'FINITE' AND (i.stockQuantity IS NULL OR i.stockQuantity = 0)")
    Page<Inventory> findOutOfStock(Pageable pageable);

    /**
     * Find in stock inventory
     *
     * @param pageable pagination information
     * @return a Page of in stock inventory
     */
    @Query("SELECT i FROM Inventory i WHERE i.trackingStrategy = 'FINITE' AND i.stockQuantity > 0")
    Page<Inventory> findInStock(Pageable pageable);
}