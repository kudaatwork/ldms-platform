package projectlx.inventory.management.repository.specification;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import projectlx.inventory.management.model.InventoryItem;
import projectlx.inventory.management.model.StockAdjustment;
import projectlx.inventory.management.model.StockAdjustment_;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.time.LocalDateTime;

public class StockAdjustmentSpecification {

    public static Specification<StockAdjustment> deleted(EntityStatus entityStatus) {
        return (root, query, cb) -> cb.notEqual(
                root.get(StockAdjustment_.entityStatus), entityStatus);
    }

    public static Specification<StockAdjustment> deleted() {
        return (root, query, cb) -> cb.notEqual(
                root.get(StockAdjustment_.entityStatus), EntityStatus.DELETED);
    }

    public static Specification<StockAdjustment> quantityDeltaGreaterThan(final double quantityDelta) {
        return (root, query, cb) -> cb.greaterThan(
                root.get(StockAdjustment_.quantityDelta), quantityDelta);
    }

    public static Specification<StockAdjustment> quantityDeltaLessThan(final double quantityDelta) {
        return (root, query, cb) -> cb.lessThan(
                root.get(StockAdjustment_.quantityDelta), quantityDelta);
    }

    public static Specification<StockAdjustment> quantityDeltaEquals(final double quantityDelta) {
        return (root, query, cb) -> cb.equal(
                root.get(StockAdjustment_.quantityDelta), quantityDelta);
    }

    public static Specification<StockAdjustment> quantityDeltaBetween(final double min, final double max) {
        return (root, query, cb) -> cb.between(
                root.get(StockAdjustment_.quantityDelta), min, max);
    }

    public static Specification<StockAdjustment> reasonLike(final String reason) {
        return (root, query, cb) -> cb.like(
                root.get(StockAdjustment_.reason), reason + "%");
    }

    public static Specification<StockAdjustment> reasonContains(final String reason) {
        return (root, query, cb) -> cb.like(
                root.get(StockAdjustment_.reason), "%" + reason + "%");
    }

    public static Specification<StockAdjustment> adjustedAtBefore(final LocalDateTime adjustedAt) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(
                root.get(StockAdjustment_.adjustedAt), adjustedAt);
    }

    public static Specification<StockAdjustment> adjustedAtAfter(final LocalDateTime adjustedAt) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(
                root.get(StockAdjustment_.adjustedAt), adjustedAt);
    }

    public static Specification<StockAdjustment> adjustedAtBetween(final LocalDateTime start, final LocalDateTime end) {
        return (root, query, cb) -> cb.between(
                root.get(StockAdjustment_.adjustedAt), start, end);
    }

    public static Specification<StockAdjustment> entityStatusEquals(final EntityStatus entityStatus) {
        return (root, query, cb) -> cb.equal(
                root.get(StockAdjustment_.entityStatus), entityStatus);
    }

    public static Specification<StockAdjustment> any(final String search) {
        return (root, query, cb) -> {
            String upper = search == null ? "" : search.toUpperCase();
            Predicate p = cb.or(
                    cb.like(cb.upper(root.get(StockAdjustment_.reason)), "%" + upper + "%")
            );
            return p;
        };
    }
}