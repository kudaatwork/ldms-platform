package projectlx.inventory.management.repository.specification;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import projectlx.inventory.management.model.PurchaseReturn;
import projectlx.inventory.management.model.PurchaseReturn_;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.time.LocalDateTime;

public class PurchaseReturnSpecification {

    // Provide both signatures: consistent with existing style and convenient default
    public static Specification<PurchaseReturn> deleted(EntityStatus entityStatus) {
        return (root, query, cb) -> cb.notLike(root.get(
                PurchaseReturn_.entityStatus).as(String.class), "%" + entityStatus + "%");
    }

    public static Specification<PurchaseReturn> deleted() {
        return (root, query, cb) -> cb.notEqual(root.get(
                PurchaseReturn_.entityStatus), EntityStatus.DELETED);
    }

    public static Specification<PurchaseReturn> returnNumberLike(final String returnNumber) {
        return (root, query, cb) -> cb.like(root.get(
                PurchaseReturn_.returnNumber).as(String.class), returnNumber + "%");
    }

    public static Specification<PurchaseReturn> reasonLike(final String reason) {
        return (root, query, cb) -> cb.like(root.get(
                PurchaseReturn_.reason).as(String.class), reason + "%");
    }

    public static Specification<PurchaseReturn> returnedByUserIdEquals(final Long returnedByUserId) {
        return (root, query, cb) -> cb.equal(root.get(
                PurchaseReturn_.returnedByUserId), returnedByUserId);
    }

    public static Specification<PurchaseReturn> createdAtEquals(final LocalDateTime createdAt) {
        return (root, query, cb) -> cb.equal(root.get(
                PurchaseReturn_.createdAt), createdAt);
    }

    public static Specification<PurchaseReturn> createdAtBetween(final LocalDateTime startDate, final LocalDateTime endDate) {
        return (root, query, cb) -> cb.between(root.get(
                PurchaseReturn_.createdAt), startDate, endDate);
    }

    public static Specification<PurchaseReturn> updatedAtEquals(final LocalDateTime updatedAt) {
        return (root, query, cb) -> cb.equal(root.get(
                PurchaseReturn_.updatedAt), updatedAt);
    }

    public static Specification<PurchaseReturn> updatedAtBetween(final LocalDateTime startDate, final LocalDateTime endDate) {
        return (root, query, cb) -> cb.between(root.get(
                PurchaseReturn_.updatedAt), startDate, endDate);
    }

    public static Specification<PurchaseReturn> entityStatusEquals(final EntityStatus entityStatus) {
        return (root, query, cb) -> cb.equal(root.get(
                PurchaseReturn_.entityStatus), entityStatus);
    }

    public static Specification<PurchaseReturn> purchaseOrderIdEquals(final Long purchaseOrderId) {
        return (root, query, cb) -> cb.equal(root.get(
                PurchaseReturn_.purchaseOrder).get("id"), purchaseOrderId);
    }

    public static Specification<PurchaseReturn> warehouseLocationIdEquals(final Long warehouseLocationId) {
        return (root, query, cb) -> cb.equal(root.get(
                PurchaseReturn_.warehouseLocation).get("id"), warehouseLocationId);
    }

    public static Specification<PurchaseReturn> any(final String search) {
        return (root, query, cb) -> {
            String upper = search == null ? "" : search.toUpperCase();
            Predicate p = cb.or(
                    cb.like(cb.upper(root.get(PurchaseReturn_.returnNumber)), "%" + upper + "%"),
                    cb.like(cb.upper(root.get(PurchaseReturn_.reason)), "%" + upper + "%")
            );
            return p;
        };
    }
}