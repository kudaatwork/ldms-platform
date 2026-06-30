package projectlx.inventory.management.repository.specification;

import org.springframework.data.jpa.domain.Specification;
import projectlx.inventory.management.model.InventoryItem;
import projectlx.inventory.management.model.InventoryItem_;
import projectlx.inventory.management.model.Product;
import projectlx.inventory.management.model.WarehouseLocation;
import projectlx.inventory.management.utils.enums.StockLevelStatus;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;

public class InventoryItemSpecification {

    public static Specification<InventoryItem> deleted(EntityStatus entityStatus) {
        return (root, query, cb) -> cb.notEqual(
                root.get(InventoryItem_.entityStatus), entityStatus);
    }

    public static Specification<InventoryItem> deleted() {
        return (root, query, cb) -> cb.notEqual(
                root.get(InventoryItem_.entityStatus), EntityStatus.DELETED);
    }

    public static Specification<InventoryItem> productEquals(final Product product) {
        return (root, query, cb) -> cb.equal(
                root.get(InventoryItem_.product), product);
    }
    
    public static Specification<InventoryItem> productIdEquals(final Long productId) {
        return (root, query, cb) -> cb.equal(
                root.get(InventoryItem_.product).get("id"), productId);
    }

    public static Specification<InventoryItem> warehouseLocationEquals(final WarehouseLocation warehouseLocation) {
        return (root, query, cb) -> cb.equal(
                root.get(InventoryItem_.warehouseLocation), warehouseLocation);
    }
    
    public static Specification<InventoryItem> warehouseLocationIdEquals(final Long warehouseLocationId) {
        return (root, query, cb) -> cb.equal(
                root.get(InventoryItem_.warehouseLocation).get("id"), warehouseLocationId);
    }

    public static Specification<InventoryItem> warehouseLocationIdIn(final Collection<Long> warehouseLocationIds) {
        if (warehouseLocationIds == null || warehouseLocationIds.isEmpty()) {
            return (root, query, cb) -> cb.disjunction();
        }
        return (root, query, cb) -> root.get(InventoryItem_.warehouseLocation).get("id").in(warehouseLocationIds);
    }
    
    public static Specification<InventoryItem> supplierIdEquals(final Long supplierId) {
        return (root, query, cb) -> cb.equal(
                root.get(InventoryItem_.supplierId), supplierId);
    }

    public static Specification<InventoryItem> batchLotLike(final String batchLot) {
        return (root, query, cb) -> cb.like(
                root.get(InventoryItem_.batchLot), batchLot + "%");
    }

    public static Specification<InventoryItem> serialNumberLike(final String serialNumber) {
        return (root, query, cb) -> cb.like(
                root.get(InventoryItem_.serialNumber), serialNumber + "%");
    }

    public static Specification<InventoryItem> expiresAtBefore(final LocalDate expiresAt) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(
                root.get(InventoryItem_.expiresAt), expiresAt);
    }

    public static Specification<InventoryItem> expiresAtAfter(final LocalDate expiresAt) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(
                root.get(InventoryItem_.expiresAt), expiresAt);
    }

    public static Specification<InventoryItem> entityStatusEquals(final EntityStatus entityStatus) {
        return (root, query, cb) -> cb.equal(
                root.get(InventoryItem_.entityStatus), entityStatus);
    }

    public static Specification<InventoryItem> any(final String search) {
        return (root, query, cb) -> {
            String upper = search == null ? "" : search.trim().toUpperCase();
            if (upper.isEmpty()) {
                return cb.conjunction();
            }
            String pattern = "%" + upper + "%";
            var productJoin = root.join(InventoryItem_.product, jakarta.persistence.criteria.JoinType.LEFT);
            var warehouseJoin = root.join(InventoryItem_.warehouseLocation, jakarta.persistence.criteria.JoinType.LEFT);
            return cb.or(
                    cb.like(cb.upper(root.get(InventoryItem_.batchLot)), pattern),
                    cb.like(cb.upper(root.get(InventoryItem_.serialNumber)), pattern),
                    cb.like(cb.upper(productJoin.get("name")), pattern),
                    cb.like(cb.upper(productJoin.get("productCode")), pattern),
                    cb.like(cb.upper(productJoin.get("barcode")), pattern),
                    cb.like(cb.upper(warehouseJoin.get("name")), pattern)
            );
        };
    }

    public static Specification<InventoryItem> stockStatusEquals(final StockLevelStatus stockStatus) {
        if (stockStatus == null) {
            return null;
        }
        return switch (stockStatus) {
            case OUT_OF_STOCK -> (root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get(InventoryItem_.currentStock), BigDecimal.ZERO);
            case LOW_STOCK -> (root, query, cb) -> cb.and(
                    cb.greaterThan(root.get(InventoryItem_.minStockLevel), BigDecimal.ZERO),
                    cb.lessThanOrEqualTo(root.get(InventoryItem_.currentStock), root.get(InventoryItem_.minStockLevel)));
            case FULLY_RESERVED -> (root, query, cb) -> cb.and(
                    cb.greaterThan(root.get(InventoryItem_.currentStock), BigDecimal.ZERO),
                    cb.greaterThanOrEqualTo(root.get(InventoryItem_.reservedQuantity), root.get(InventoryItem_.currentStock)));
            case IN_STOCK -> (root, query, cb) -> cb.and(
                    cb.greaterThan(root.get(InventoryItem_.currentStock), BigDecimal.ZERO),
                    cb.or(
                            cb.lessThanOrEqualTo(root.get(InventoryItem_.minStockLevel), BigDecimal.ZERO),
                            cb.greaterThan(root.get(InventoryItem_.currentStock), root.get(InventoryItem_.minStockLevel))),
                    cb.lessThan(root.get(InventoryItem_.reservedQuantity), root.get(InventoryItem_.currentStock)));
        };
    }
}
