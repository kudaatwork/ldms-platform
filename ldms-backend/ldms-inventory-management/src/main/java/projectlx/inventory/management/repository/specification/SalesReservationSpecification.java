package projectlx.inventory.management.repository.specification;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import projectlx.inventory.management.model.Product;
import projectlx.inventory.management.model.ReservationStatus;
import projectlx.inventory.management.model.SalesReservation;
import projectlx.inventory.management.model.SalesReservation_;
import projectlx.inventory.management.model.WarehouseLocation;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SalesReservationSpecification {

    public static Specification<SalesReservation> deleted(EntityStatus entityStatus) {
        return (root, query, cb) -> cb.notEqual(
                root.get(SalesReservation_.entityStatus), entityStatus);
    }

    public static Specification<SalesReservation> deleted() {
        return (root, query, cb) -> cb.notEqual(
                root.get(SalesReservation_.entityStatus), EntityStatus.DELETED);
    }

    public static Specification<SalesReservation> reservationNumberLike(final String reservationNumber) {
        return (root, query, cb) -> cb.like(
                root.get(SalesReservation_.reservationNumber), reservationNumber + "%");
    }

    public static Specification<SalesReservation> customerIdEquals(final Long customerId) {
        return (root, query, cb) -> cb.equal(
                root.get(SalesReservation_.customerId), customerId);
    }

    public static Specification<SalesReservation> productEquals(final Product product) {
        return (root, query, cb) -> cb.equal(
                root.get(SalesReservation_.product), product);
    }

    public static Specification<SalesReservation> productIdEquals(final Long productId) {
        return (root, query, cb) -> cb.equal(
                root.get(SalesReservation_.product).get("id"), productId);
    }

    public static Specification<SalesReservation> warehouseLocationEquals(final WarehouseLocation warehouseLocation) {
        return (root, query, cb) -> cb.equal(
                root.get(SalesReservation_.warehouseLocation), warehouseLocation);
    }

    public static Specification<SalesReservation> warehouseLocationIdEquals(final Long warehouseLocationId) {
        return (root, query, cb) -> cb.equal(
                root.get(SalesReservation_.warehouseLocation).get("id"), warehouseLocationId);
    }

    public static Specification<SalesReservation> quantityReservedGreaterThan(final BigDecimal quantityReserved) {
        return (root, query, cb) -> cb.greaterThan(
                root.get(SalesReservation_.quantityReserved), quantityReserved);
    }

    public static Specification<SalesReservation> quantityReservedLessThan(final BigDecimal quantityReserved) {
        return (root, query, cb) -> cb.lessThan(
                root.get(SalesReservation_.quantityReserved), quantityReserved);
    }

    public static Specification<SalesReservation> quantityReservedBetween(final BigDecimal minQuantity, final BigDecimal maxQuantity) {
        return (root, query, cb) -> cb.between(
                root.get(SalesReservation_.quantityReserved), minQuantity, maxQuantity);
    }

    public static Specification<SalesReservation> reservedUntilBefore(final LocalDateTime reservedUntil) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(
                root.get(SalesReservation_.reservedUntil), reservedUntil);
    }

    public static Specification<SalesReservation> reservedUntilAfter(final LocalDateTime reservedUntil) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(
                root.get(SalesReservation_.reservedUntil), reservedUntil);
    }

    public static Specification<SalesReservation> reservedUntilIsNull() {
        return (root, query, cb) -> cb.isNull(
                root.get(SalesReservation_.reservedUntil));
    }

    public static Specification<SalesReservation> reservedUntilIsNotNull() {
        return (root, query, cb) -> cb.isNotNull(
                root.get(SalesReservation_.reservedUntil));
    }

    public static Specification<SalesReservation> reservationStatusEquals(final ReservationStatus reservationStatus) {
        return (root, query, cb) -> cb.equal(
                root.get(SalesReservation_.reservationStatus), reservationStatus);
    }

    public static Specification<SalesReservation> createdByUserIdEquals(final Long createdByUserId) {
        return (root, query, cb) -> cb.equal(
                root.get(SalesReservation_.createdByUserId), createdByUserId);
    }

    public static Specification<SalesReservation> updatedByUserIdEquals(final Long updatedByUserId) {
        return (root, query, cb) -> cb.equal(
                root.get(SalesReservation_.updatedByUserId), updatedByUserId);
    }

    public static Specification<SalesReservation> entityStatusEquals(final EntityStatus entityStatus) {
        return (root, query, cb) -> cb.equal(
                root.get(SalesReservation_.entityStatus), entityStatus);
    }

    public static Specification<SalesReservation> createdAtBetween(final LocalDateTime startDate, final LocalDateTime endDate) {
        return (root, query, cb) -> cb.between(
                root.get(SalesReservation_.createdAt), startDate, endDate);
    }

    public static Specification<SalesReservation> updatedAtBetween(final LocalDateTime startDate, final LocalDateTime endDate) {
        return (root, query, cb) -> cb.between(
                root.get(SalesReservation_.updatedAt), startDate, endDate);
    }

    // Business logic specifications

    // Check if reservation is expired
    public static Specification<SalesReservation> isExpired() {
        return (root, query, cb) -> cb.and(
                cb.isNotNull(root.get(SalesReservation_.reservedUntil)),
                cb.lessThan(root.get(SalesReservation_.reservedUntil), LocalDateTime.now())
        );
    }

    // Check if reservation is not expired (either no expiry or future expiry)
    public static Specification<SalesReservation> isNotExpired() {
        return (root, query, cb) -> cb.or(
                cb.isNull(root.get(SalesReservation_.reservedUntil)),
                cb.greaterThanOrEqualTo(root.get(SalesReservation_.reservedUntil), LocalDateTime.now())
        );
    }

    // Check if reservation is active (status is ACTIVE and not expired)
    public static Specification<SalesReservation> isActive() {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get(SalesReservation_.reservationStatus), ReservationStatus.ACTIVE),
                cb.or(
                        cb.isNull(root.get(SalesReservation_.reservedUntil)),
                        cb.greaterThanOrEqualTo(root.get(SalesReservation_.reservedUntil), LocalDateTime.now())
                )
        );
    }

    // Find reservations expiring within a certain time period
    public static Specification<SalesReservation> expiringWithin(final LocalDateTime timeFrame) {
        return (root, query, cb) -> cb.and(
                cb.isNotNull(root.get(SalesReservation_.reservedUntil)),
                cb.between(root.get(SalesReservation_.reservedUntil), LocalDateTime.now(), timeFrame)
        );
    }

    public static Specification<SalesReservation> any(final String search) {
        return (root, query, cb) -> {
            String upper = search == null ? "" : search.toUpperCase();
            Predicate p = cb.or(
                    cb.like(cb.upper(root.get(SalesReservation_.reservationNumber)), "%" + upper + "%"),
                    cb.like(cb.upper(root.get(SalesReservation_.notes)), "%" + upper + "%"),
                    cb.like(cb.upper(root.get(SalesReservation_.product).get("name")), "%" + upper + "%"),
                    cb.like(cb.upper(root.get(SalesReservation_.product).get("sku")), "%" + upper + "%")
            );
            return p;
        };
    }
}