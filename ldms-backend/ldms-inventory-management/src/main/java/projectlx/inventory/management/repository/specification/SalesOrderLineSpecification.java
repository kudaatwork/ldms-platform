package projectlx.inventory.management.repository.specification;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import projectlx.inventory.management.model.Product;
import projectlx.inventory.management.model.SalesOrder;
import projectlx.inventory.management.model.SalesOrderLine;
import projectlx.inventory.management.model.SalesOrderLine_;
import projectlx.inventory.management.model.UnitOfMeasure;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SalesOrderLineSpecification {

    public static Specification<SalesOrderLine> deleted(EntityStatus entityStatus) {
        return (root, query, cb) -> cb.notEqual(
                root.get(SalesOrderLine_.entityStatus), entityStatus);
    }

    public static Specification<SalesOrderLine> deleted() {
        return (root, query, cb) -> cb.notEqual(
                root.get(SalesOrderLine_.entityStatus), EntityStatus.DELETED);
    }

    public static Specification<SalesOrderLine> salesOrderEquals(final SalesOrder salesOrder) {
        return (root, query, cb) -> cb.equal(
                root.get(SalesOrderLine_.salesOrder), salesOrder);
    }

    public static Specification<SalesOrderLine> salesOrderIdEquals(final Long salesOrderId) {
        return (root, query, cb) -> cb.equal(
                root.get(SalesOrderLine_.salesOrder).get("id"), salesOrderId);
    }

    public static Specification<SalesOrderLine> productEquals(final Product product) {
        return (root, query, cb) -> cb.equal(
                root.get(SalesOrderLine_.product), product);
    }

    public static Specification<SalesOrderLine> productIdEquals(final Long productId) {
        return (root, query, cb) -> cb.equal(
                root.get(SalesOrderLine_.product).get("id"), productId);
    }

    public static Specification<SalesOrderLine> quantityGreaterThan(final BigDecimal quantity) {
        return (root, query, cb) -> cb.greaterThan(
                root.get(SalesOrderLine_.quantity), quantity);
    }

    public static Specification<SalesOrderLine> quantityLessThan(final BigDecimal quantity) {
        return (root, query, cb) -> cb.lessThan(
                root.get(SalesOrderLine_.quantity), quantity);
    }

    public static Specification<SalesOrderLine> quantityBetween(final BigDecimal minQuantity, final BigDecimal maxQuantity) {
        return (root, query, cb) -> cb.between(
                root.get(SalesOrderLine_.quantity), minQuantity, maxQuantity);
    }

    public static Specification<SalesOrderLine> unitPriceGreaterThan(final BigDecimal unitPrice) {
        return (root, query, cb) -> cb.greaterThan(
                root.get(SalesOrderLine_.unitPrice), unitPrice);
    }

    public static Specification<SalesOrderLine> unitPriceLessThan(final BigDecimal unitPrice) {
        return (root, query, cb) -> cb.lessThan(
                root.get(SalesOrderLine_.unitPrice), unitPrice);
    }

    public static Specification<SalesOrderLine> unitPriceBetween(final BigDecimal minPrice, final BigDecimal maxPrice) {
        return (root, query, cb) -> cb.between(
                root.get(SalesOrderLine_.unitPrice), minPrice, maxPrice);
    }

    public static Specification<SalesOrderLine> totalPriceGreaterThan(final BigDecimal totalPrice) {
        return (root, query, cb) -> cb.greaterThan(
                root.get(SalesOrderLine_.totalPrice), totalPrice);
    }

    public static Specification<SalesOrderLine> totalPriceLessThan(final BigDecimal totalPrice) {
        return (root, query, cb) -> cb.lessThan(
                root.get(SalesOrderLine_.totalPrice), totalPrice);
    }

    public static Specification<SalesOrderLine> totalPriceBetween(final BigDecimal minPrice, final BigDecimal maxPrice) {
        return (root, query, cb) -> cb.between(
                root.get(SalesOrderLine_.totalPrice), minPrice, maxPrice);
    }

    public static Specification<SalesOrderLine> fulfilledQuantityGreaterThan(final BigDecimal fulfilledQuantity) {
        return (root, query, cb) -> cb.greaterThan(
                root.get(SalesOrderLine_.fulfilledQuantity), fulfilledQuantity);
    }

    public static Specification<SalesOrderLine> fulfilledQuantityLessThan(final BigDecimal fulfilledQuantity) {
        return (root, query, cb) -> cb.lessThan(
                root.get(SalesOrderLine_.fulfilledQuantity), fulfilledQuantity);
    }

    public static Specification<SalesOrderLine> fulfilledQuantityIsNull() {
        return (root, query, cb) -> cb.isNull(
                root.get(SalesOrderLine_.fulfilledQuantity));
    }

    public static Specification<SalesOrderLine> fulfilledQuantityIsNotNull() {
        return (root, query, cb) -> cb.isNotNull(
                root.get(SalesOrderLine_.fulfilledQuantity));
    }

    public static Specification<SalesOrderLine> unitOfMeasureEquals(final UnitOfMeasure unitOfMeasure) {
        return (root, query, cb) -> cb.equal(
                root.get(SalesOrderLine_.unitOfMeasure), unitOfMeasure);
    }

    public static Specification<SalesOrderLine> createdByUserIdEquals(final Long createdByUserId) {
        return (root, query, cb) -> cb.equal(
                root.get(SalesOrderLine_.createdByUserId), createdByUserId);
    }

    public static Specification<SalesOrderLine> updatedByUserIdEquals(final Long updatedByUserId) {
        return (root, query, cb) -> cb.equal(
                root.get(SalesOrderLine_.updatedByUserId), updatedByUserId);
    }

    public static Specification<SalesOrderLine> entityStatusEquals(final EntityStatus entityStatus) {
        return (root, query, cb) -> cb.equal(
                root.get(SalesOrderLine_.entityStatus), entityStatus);
    }

    public static Specification<SalesOrderLine> createdAtBetween(final LocalDateTime startDate, final LocalDateTime endDate) {
        return (root, query, cb) -> cb.between(
                root.get(SalesOrderLine_.createdAt), startDate, endDate);
    }

    // Check if line is fully fulfilled
    public static Specification<SalesOrderLine> isFullyFulfilled() {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(
                root.get(SalesOrderLine_.fulfilledQuantity),
                root.get(SalesOrderLine_.quantity));
    }

    // Check if line is partially fulfilled
    public static Specification<SalesOrderLine> isPartiallyFulfilled() {
        return (root, query, cb) -> cb.and(
                cb.isNotNull(root.get(SalesOrderLine_.fulfilledQuantity)),
                cb.greaterThan(root.get(SalesOrderLine_.fulfilledQuantity), BigDecimal.ZERO),
                cb.lessThan(root.get(SalesOrderLine_.fulfilledQuantity), root.get(SalesOrderLine_.quantity))
        );
    }

    // Check if line is not fulfilled
    public static Specification<SalesOrderLine> isNotFulfilled() {
        return (root, query, cb) -> cb.or(
                cb.isNull(root.get(SalesOrderLine_.fulfilledQuantity)),
                cb.equal(root.get(SalesOrderLine_.fulfilledQuantity), BigDecimal.ZERO)
        );
    }

    public static Specification<SalesOrderLine> any(final String search) {
        return (root, query, cb) -> {
            String upper = search == null ? "" : search.toUpperCase();
            // Since we don't have direct text fields in SalesOrderLine, we'll search related entities
            Predicate p = cb.or(
                    cb.like(cb.upper(root.get(SalesOrderLine_.salesOrder).get("salesOrderNumber")), "%" + upper + "%"),
                    cb.like(cb.upper(root.get(SalesOrderLine_.product).get("name")), "%" + upper + "%")
            );
            return p;
        };
    }
}