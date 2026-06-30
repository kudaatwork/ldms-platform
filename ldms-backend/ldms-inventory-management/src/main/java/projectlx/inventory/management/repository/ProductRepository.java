package projectlx.inventory.management.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import projectlx.inventory.management.model.Product;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Product> findByIdAndEntityStatusNot(Long id, EntityStatus entityStatus);
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Product> findByProductCodeAndEntityStatusNot(String productCode, EntityStatus entityStatus);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE TRIM(p.productCode) = :productCode AND p.entityStatus <> :entityStatus")
    Optional<Product> findByProductCodeTrimmedAndEntityStatusNot(
            @Param("productCode") String productCode,
            @Param("entityStatus") EntityStatus entityStatus);

    /** Read-only lookup for CSV import (no pessimistic lock; safe outside a transaction). */
    @Query("SELECT p FROM Product p WHERE TRIM(p.productCode) = :productCode AND p.entityStatus <> :entityStatus")
    Optional<Product> lookupByProductCodeTrimmedAndEntityStatusNot(
            @Param("productCode") String productCode,
            @Param("entityStatus") EntityStatus entityStatus);

    /** Read-only lookup for CSV import (no pessimistic lock; safe outside a transaction). */
    @Query("SELECT p FROM Product p WHERE p.productCode = :productCode AND p.entityStatus <> :entityStatus")
    Optional<Product> lookupByProductCodeAndEntityStatusNot(
            @Param("productCode") String productCode,
            @Param("entityStatus") EntityStatus entityStatus);

    List<Product> findByEntityStatusNot(EntityStatus entityStatus);

    List<Product> findBySupplierIdAndEntityStatusNot(Long supplierId, EntityStatus entityStatus);

    List<Product> findByIdInAndEntityStatusNot(java.util.Collection<Long> ids, EntityStatus entityStatus);

    @Query("SELECT p FROM Product p WHERE TRIM(p.barcode) = :barcode AND p.entityStatus <> :entityStatus")
    Optional<Product> lookupByBarcodeTrimmedAndEntityStatusNot(
            @Param("barcode") String barcode,
            @Param("entityStatus") EntityStatus entityStatus);

    @Query("SELECT p FROM Product p WHERE p.barcode = :barcode AND p.entityStatus <> :entityStatus")
    Optional<Product> lookupByBarcodeAndEntityStatusNot(
            @Param("barcode") String barcode,
            @Param("entityStatus") EntityStatus entityStatus);
}
