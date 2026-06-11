package projectlx.inventory.management.repository.specification;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import projectlx.inventory.management.model.InventoryItem;
import projectlx.inventory.management.model.ReferenceDocumentType;
import projectlx.inventory.management.model.StockTransactionHistory;
import projectlx.inventory.management.model.StockTransactionHistory_;
import projectlx.inventory.management.model.TransactionType;
import projectlx.inventory.management.model.WarehouseLocation;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class StockTransactionHistorySpecification {

    public static Specification<StockTransactionHistory> deleted(EntityStatus entityStatus) {
        return (root, query, cb) -> cb.notEqual(
                root.get(StockTransactionHistory_.entityStatus), entityStatus);
    }

    public static Specification<StockTransactionHistory> deleted() {
        return (root, query, cb) -> cb.notEqual(
                root.get(StockTransactionHistory_.entityStatus), EntityStatus.DELETED);
    }

    public static Specification<StockTransactionHistory> inventoryItemEquals(final InventoryItem inventoryItem) {
        return (root, query, cb) -> cb.equal(
                root.get(StockTransactionHistory_.inventoryItem), inventoryItem);
    }

    public static Specification<StockTransactionHistory> transactionTypeEquals(final TransactionType transactionType) {
        return (root, query, cb) -> cb.equal(
                root.get(StockTransactionHistory_.transactionType), transactionType);
    }

    public static Specification<StockTransactionHistory> warehouseLocationEquals(final WarehouseLocation warehouseLocation) {
        return (root, query, cb) -> cb.equal(
                root.get(StockTransactionHistory_.warehouseLocation), warehouseLocation);
    }

    public static Specification<StockTransactionHistory> performedByUserIdEquals(final Long performedByUserId) {
        return (root, query, cb) -> cb.equal(
                root.get(StockTransactionHistory_.performedByUserId), performedByUserId);
    }

    public static Specification<StockTransactionHistory> referenceDocumentIdEquals(final Long referenceDocumentId) {
        return (root, query, cb) -> cb.equal(
                root.get(StockTransactionHistory_.referenceDocumentId), referenceDocumentId);
    }

    public static Specification<StockTransactionHistory> referenceDocumentTypeEquals(final ReferenceDocumentType referenceDocumentType) {
        return (root, query, cb) -> cb.equal(
                root.get(StockTransactionHistory_.referenceDocumentType), referenceDocumentType);
    }

    public static Specification<StockTransactionHistory> referenceDocumentTypeLike(final String referenceDocumentType) {
        return (root, query, cb) -> cb.like(
                root.get(StockTransactionHistory_.referenceDocumentType).as(String.class), referenceDocumentType + "%");
    }

    public static Specification<StockTransactionHistory> reasonLike(final String reason) {
        return (root, query, cb) -> cb.like(
                root.get(StockTransactionHistory_.reason), reason + "%");
    }

    public static Specification<StockTransactionHistory> timestampBefore(final LocalDateTime timestamp) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(
                root.get(StockTransactionHistory_.timestamp), timestamp);
    }

    public static Specification<StockTransactionHistory> timestampAfter(final LocalDateTime timestamp) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(
                root.get(StockTransactionHistory_.timestamp), timestamp);
    }

    public static Specification<StockTransactionHistory> timestampBetween(final LocalDateTime start, final LocalDateTime end) {
        return (root, query, cb) -> cb.between(
                root.get(StockTransactionHistory_.timestamp), start, end);
    }

    public static Specification<StockTransactionHistory> quantityChangeGreaterThan(final BigDecimal quantityChange) {
        return (root, query, cb) -> cb.greaterThan(
                root.get(StockTransactionHistory_.quantityChange), quantityChange);
    }

    public static Specification<StockTransactionHistory> quantityChangeLessThan(final BigDecimal quantityChange) {
        return (root, query, cb) -> cb.lessThan(
                root.get(StockTransactionHistory_.quantityChange), quantityChange);
    }

    public static Specification<StockTransactionHistory> entityStatusEquals(final EntityStatus entityStatus) {
        return (root, query, cb) -> cb.equal(
                root.get(StockTransactionHistory_.entityStatus), entityStatus);
    }

    public static Specification<StockTransactionHistory> any(final String search) {
        return (root, query, cb) -> {
            String upper = search == null ? "" : search.toUpperCase();
            Predicate p = cb.or(
                    cb.like(cb.upper(root.get(StockTransactionHistory_.referenceDocumentType)), "%" + upper + "%"),
                    cb.like(cb.upper(root.get(StockTransactionHistory_.reason)), "%" + upper + "%")
            );
            return p;
        };
    }
}