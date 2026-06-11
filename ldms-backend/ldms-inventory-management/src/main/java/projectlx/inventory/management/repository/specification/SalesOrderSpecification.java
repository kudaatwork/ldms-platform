package projectlx.inventory.management.repository.specification;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import projectlx.inventory.management.model.SalesOrder;
import projectlx.inventory.management.model.SalesOrder_;
import projectlx.inventory.management.model.SalesOrderStatus;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class SalesOrderSpecification {

    public static Specification<SalesOrder> deleted(EntityStatus entityStatus) {
        return (root, query, cb) -> cb.notEqual(
                root.get(SalesOrder_.entityStatus), entityStatus);
    }

    public static Specification<SalesOrder> deleted() {
        return (root, query, cb) -> cb.notEqual(
                root.get(SalesOrder_.entityStatus), EntityStatus.DELETED);
    }

    public static Specification<SalesOrder> salesOrderNumberLike(final String salesOrderNumber) {
        return (root, query, cb) -> cb.like(
                root.get(SalesOrder_.salesOrderNumber), salesOrderNumber + "%");
    }

    public static Specification<SalesOrder> customerIdEquals(final Long customerId) {
        return (root, query, cb) -> cb.equal(
                root.get(SalesOrder_.customerId), customerId);
    }

    public static Specification<SalesOrder> supplierOrganizationIdEquals(final Long supplierOrganizationId) {
        return (root, query, cb) -> cb.equal(
                root.get("supplierOrganizationId"), supplierOrganizationId);
    }

    public static Specification<SalesOrder> statusEquals(final SalesOrderStatus status) {
        return (root, query, cb) -> cb.equal(
                root.get(SalesOrder_.status), status);
    }

    public static Specification<SalesOrder> orderDateBetween(final LocalDate startDate, final LocalDate endDate) {
        return (root, query, cb) -> cb.between(
                root.get(SalesOrder_.orderDate), startDate, endDate);
    }

    public static Specification<SalesOrder> orderDateBefore(final LocalDate orderDate) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(
                root.get(SalesOrder_.orderDate), orderDate);
    }

    public static Specification<SalesOrder> orderDateAfter(final LocalDate orderDate) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(
                root.get(SalesOrder_.orderDate), orderDate);
    }

    public static Specification<SalesOrder> expectedDeliveryDateBefore(final LocalDate expectedDeliveryDate) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(
                root.get(SalesOrder_.expectedDeliveryDate), expectedDeliveryDate);
    }

    public static Specification<SalesOrder> expectedDeliveryDateAfter(final LocalDate expectedDeliveryDate) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(
                root.get(SalesOrder_.expectedDeliveryDate), expectedDeliveryDate);
    }

    public static Specification<SalesOrder> deliveredDateBefore(final LocalDateTime deliveredDate) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(
                root.get(SalesOrder_.deliveredDate), deliveredDate);
    }

    public static Specification<SalesOrder> deliveredDateAfter(final LocalDateTime deliveredDate) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(
                root.get(SalesOrder_.deliveredDate), deliveredDate);
    }

    public static Specification<SalesOrder> totalAmountGreaterThan(final BigDecimal totalAmount) {
        return (root, query, cb) -> cb.greaterThan(
                root.get(SalesOrder_.totalAmount), totalAmount);
    }

    public static Specification<SalesOrder> totalAmountLessThan(final BigDecimal totalAmount) {
        return (root, query, cb) -> cb.lessThan(
                root.get(SalesOrder_.totalAmount), totalAmount);
    }

    public static Specification<SalesOrder> totalAmountBetween(final BigDecimal minAmount, final BigDecimal maxAmount) {
        return (root, query, cb) -> cb.between(
                root.get(SalesOrder_.totalAmount), minAmount, maxAmount);
    }

    public static Specification<SalesOrder> createdByUserIdEquals(final Long createdByUserId) {
        return (root, query, cb) -> cb.equal(
                root.get(SalesOrder_.createdByUserId), createdByUserId);
    }

    public static Specification<SalesOrder> updatedByUserIdEquals(final Long updatedByUserId) {
        return (root, query, cb) -> cb.equal(
                root.get(SalesOrder_.updatedByUserId), updatedByUserId);
    }

    public static Specification<SalesOrder> entityStatusEquals(final EntityStatus entityStatus) {
        return (root, query, cb) -> cb.equal(
                root.get(SalesOrder_.entityStatus), entityStatus);
    }

    public static Specification<SalesOrder> createdAtBetween(final LocalDateTime startDate, final LocalDateTime endDate) {
        return (root, query, cb) -> cb.between(
                root.get(SalesOrder_.createdAt), startDate, endDate);
    }

    public static Specification<SalesOrder> any(final String search) {
        return (root, query, cb) -> {
            String upper = search == null ? "" : search.toUpperCase();
            Predicate p = cb.or(
                    cb.like(cb.upper(root.get(SalesOrder_.salesOrderNumber)), "%" + upper + "%"),
                    cb.like(cb.upper(root.get(SalesOrder_.notes)), "%" + upper + "%")
            );
            return p;
        };
    }
}
