package projectlx.inventory.management.repository.specification;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import projectlx.inventory.management.model.*;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * JPA Specifications for Purchase Requisition queries.
 * Provides type-safe, composable query building.
 */
public class PurchaseRequisitionSpecification {

    public static Specification<PurchaseRequisition> deleted(EntityStatus entityStatus) {
        return (root, query, cb) -> cb.notEqual(
                root.get(PurchaseRequisition_.entityStatus), entityStatus);
    }

    public static Specification<PurchaseRequisition> deleted() {
        return (root, query, cb) -> cb.notEqual(
                root.get(PurchaseRequisition_.entityStatus), EntityStatus.DELETED);
    }

    public static Specification<PurchaseRequisition> requisitionNumberLike(final String requisitionNumber) {
        return (root, query, cb) -> cb.like(
                root.get(PurchaseRequisition_.requisitionNumber), requisitionNumber + "%");
    }

    public static Specification<PurchaseRequisition> organizationIdEquals(final Long organizationId) {
        return (root, query, cb) -> cb.equal(
                root.get(PurchaseRequisition_.organizationId), organizationId);
    }

    public static Specification<PurchaseRequisition> departmentIdEquals(final Long departmentId) {
        return (root, query, cb) -> cb.equal(
                root.get(PurchaseRequisition_.departmentId), departmentId);
    }

    public static Specification<PurchaseRequisition> requestedByUserIdEquals(final Long requestedByUserId) {
        return (root, query, cb) -> cb.equal(
                root.get(PurchaseRequisition_.requestedByUserId), requestedByUserId);
    }

    public static Specification<PurchaseRequisition> statusEquals(final PurchaseRequisitionStatus status) {
        return (root, query, cb) -> cb.equal(
                root.get(PurchaseRequisition_.status), status);
    }

    public static Specification<PurchaseRequisition> statusIn(final PurchaseRequisitionStatus... statuses) {
        return (root, query, cb) -> root.get(PurchaseRequisition_.status).in((Object[]) statuses);
    }

    public static Specification<PurchaseRequisition> priorityEquals(final PriorityLevel priority) {
        return (root, query, cb) -> cb.equal(
                root.get(PurchaseRequisition_.priority), priority);
    }

    public static Specification<PurchaseRequisition> requisitionDateBetween(final LocalDate startDate, final LocalDate endDate) {
        return (root, query, cb) -> cb.between(
                root.get(PurchaseRequisition_.requisitionDate), startDate, endDate);
    }

    public static Specification<PurchaseRequisition> requisitionDateBefore(final LocalDate requisitionDate) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(
                root.get(PurchaseRequisition_.requisitionDate), requisitionDate);
    }

    public static Specification<PurchaseRequisition> requisitionDateAfter(final LocalDate requisitionDate) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(
                root.get(PurchaseRequisition_.requisitionDate), requisitionDate);
    }

    public static Specification<PurchaseRequisition> requiredByDateBefore(final LocalDate requiredByDate) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(
                root.get(PurchaseRequisition_.requiredByDate), requiredByDate);
    }

    public static Specification<PurchaseRequisition> requiredByDateAfter(final LocalDate requiredByDate) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(
                root.get(PurchaseRequisition_.requiredByDate), requiredByDate);
    }

    public static Specification<PurchaseRequisition> expiryDateBefore(final LocalDate expiryDate) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(
                root.get(PurchaseRequisition_.expiryDate), expiryDate);
    }

    public static Specification<PurchaseRequisition> expiryDateAfter(final LocalDate expiryDate) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(
                root.get(PurchaseRequisition_.expiryDate), expiryDate);
    }

    public static Specification<PurchaseRequisition> approvedByUserIdEquals(final Long approvedByUserId) {
        return (root, query, cb) -> cb.equal(
                root.get(PurchaseRequisition_.approvedByUserId), approvedByUserId);
    }

    public static Specification<PurchaseRequisition> approvedAtBetween(final LocalDateTime startDate, final LocalDateTime endDate) {
        return (root, query, cb) -> cb.between(
                root.get(PurchaseRequisition_.approvedAt), startDate, endDate);
    }

    public static Specification<PurchaseRequisition> estimatedTotalGreaterThan(final BigDecimal estimatedTotal) {
        return (root, query, cb) -> cb.greaterThan(
                root.get(PurchaseRequisition_.estimatedTotal), estimatedTotal);
    }

    public static Specification<PurchaseRequisition> estimatedTotalLessThan(final BigDecimal estimatedTotal) {
        return (root, query, cb) -> cb.lessThan(
                root.get(PurchaseRequisition_.estimatedTotal), estimatedTotal);
    }

    public static Specification<PurchaseRequisition> estimatedTotalBetween(final BigDecimal minAmount, final BigDecimal maxAmount) {
        return (root, query, cb) -> cb.between(
                root.get(PurchaseRequisition_.estimatedTotal), minAmount, maxAmount);
    }

    public static Specification<PurchaseRequisition> budgetAvailableEquals(final Boolean budgetAvailable) {
        return (root, query, cb) -> cb.equal(
                root.get(PurchaseRequisition_.budgetAvailable), budgetAvailable);
    }

    public static Specification<PurchaseRequisition> costCenterEquals(final String costCenter) {
        return (root, query, cb) -> cb.equal(
                root.get(PurchaseRequisition_.costCenter), costCenter);
    }

    public static Specification<PurchaseRequisition> projectCodeEquals(final String projectCode) {
        return (root, query, cb) -> cb.equal(
                root.get(PurchaseRequisition_.projectCode), projectCode);
    }

    public static Specification<PurchaseRequisition> preferredSupplierIdEquals(final Long preferredSupplierId) {
        return (root, query, cb) -> cb.equal(
                root.get(PurchaseRequisition_.preferredSupplierId), preferredSupplierId);
    }

    public static Specification<PurchaseRequisition> targetWarehouseIdEquals(final Long targetWarehouseId) {
        return (root, query, cb) -> cb.equal(
                root.get(PurchaseRequisition_.targetWarehouseId), targetWarehouseId);
    }

    public static Specification<PurchaseRequisition> defaultFulfillmentMethodEquals(final FulfillmentMethod fulfillmentMethod) {
        return (root, query, cb) -> cb.equal(
                root.get(PurchaseRequisition_.defaultFulfillmentMethod), fulfillmentMethod);
    }

    public static Specification<PurchaseRequisition> createdByUserIdEquals(final Long createdByUserId) {
        return (root, query, cb) -> cb.equal(
                root.get(PurchaseRequisition_.createdByUserId), createdByUserId);
    }

    public static Specification<PurchaseRequisition> updatedByUserIdEquals(final Long updatedByUserId) {
        return (root, query, cb) -> cb.equal(
                root.get(PurchaseRequisition_.updatedByUserId), updatedByUserId);
    }

    public static Specification<PurchaseRequisition> entityStatusEquals(final EntityStatus entityStatus) {
        return (root, query, cb) -> cb.equal(
                root.get(PurchaseRequisition_.entityStatus), entityStatus);
    }

    public static Specification<PurchaseRequisition> createdAtBetween(final LocalDateTime startDate, final LocalDateTime endDate) {
        return (root, query, cb) -> cb.between(
                root.get(PurchaseRequisition_.createdAt), startDate, endDate);
    }

    /**
     * Search across multiple fields (requisition number, purpose, justification, notes)
     */
    public static Specification<PurchaseRequisition> any(final String search) {
        return (root, query, cb) -> {
            String upper = search == null ? "" : search.toUpperCase();
            Predicate p = cb.or(
                    cb.like(cb.upper(root.get(PurchaseRequisition_.requisitionNumber)), "%" + upper + "%"),
                    cb.like(cb.upper(root.get(PurchaseRequisition_.purpose)), "%" + upper + "%"),
                    cb.like(cb.upper(root.get(PurchaseRequisition_.justification)), "%" + upper + "%"),
                    cb.like(cb.upper(root.get(PurchaseRequisition_.costCenter)), "%" + upper + "%"),
                    cb.like(cb.upper(root.get(PurchaseRequisition_.projectCode)), "%" + upper + "%")
            );
            return p;
        };
    }
}
