package projectlx.inventory.management.model;

import jakarta.persistence.metamodel.ListAttribute;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * JPA Metamodel for PurchaseRequisition entity.
 * Used for type-safe queries with Criteria API and Specifications.
 */
@StaticMetamodel(PurchaseRequisition.class)
public class PurchaseRequisition_ {
    public static volatile SingularAttribute<PurchaseRequisition, Long> id;
    public static volatile SingularAttribute<PurchaseRequisition, String> requisitionNumber;
    public static volatile SingularAttribute<PurchaseRequisition, String> externalReference;
    public static volatile SingularAttribute<PurchaseRequisition, Integer> version;

    // Requester information
    public static volatile SingularAttribute<PurchaseRequisition, Long> organizationId;
    public static volatile SingularAttribute<PurchaseRequisition, Long> departmentId;
    public static volatile SingularAttribute<PurchaseRequisition, Long> requestedByUserId;
    public static volatile SingularAttribute<PurchaseRequisition, String> costCenter;
    public static volatile SingularAttribute<PurchaseRequisition, String> projectCode;

    // Purpose & justification
    public static volatile SingularAttribute<PurchaseRequisition, String> purpose;
    public static volatile SingularAttribute<PurchaseRequisition, String> justification;
    public static volatile SingularAttribute<PurchaseRequisition, PriorityLevel> priority;

    // Dates
    public static volatile SingularAttribute<PurchaseRequisition, LocalDate> requisitionDate;
    public static volatile SingularAttribute<PurchaseRequisition, LocalDate> requiredByDate;
    public static volatile SingularAttribute<PurchaseRequisition, LocalDate> expiryDate;

    // Status & workflow
    public static volatile SingularAttribute<PurchaseRequisition, PurchaseRequisitionStatus> status;
    public static volatile SingularAttribute<PurchaseRequisition, LocalDateTime> submittedAt;
    public static volatile SingularAttribute<PurchaseRequisition, Long> submittedByUserId;
    public static volatile SingularAttribute<PurchaseRequisition, Long> approvedByUserId;
    public static volatile SingularAttribute<PurchaseRequisition, LocalDateTime> approvedAt;
    public static volatile SingularAttribute<PurchaseRequisition, String> approvalNotes;
    public static volatile SingularAttribute<PurchaseRequisition, Long> rejectedByUserId;
    public static volatile SingularAttribute<PurchaseRequisition, LocalDateTime> rejectedAt;
    public static volatile SingularAttribute<PurchaseRequisition, String> rejectionReason;

    // Fulfillment
    public static volatile SingularAttribute<PurchaseRequisition, FulfillmentMethod> defaultFulfillmentMethod;
    public static volatile SingularAttribute<PurchaseRequisition, Long> targetWarehouseId;
    public static volatile SingularAttribute<PurchaseRequisition, Long> preferredSupplierId;

    // Financial
    public static volatile SingularAttribute<PurchaseRequisition, BigDecimal> estimatedTotal;
    public static volatile SingularAttribute<PurchaseRequisition, String> currency;
    public static volatile SingularAttribute<PurchaseRequisition, Boolean> budgetAvailable;
    public static volatile SingularAttribute<PurchaseRequisition, String> budgetCode;

    // Audit fields
    public static volatile SingularAttribute<PurchaseRequisition, Long> createdByUserId;
    public static volatile SingularAttribute<PurchaseRequisition, Long> updatedByUserId;
    public static volatile SingularAttribute<PurchaseRequisition, LocalDateTime> createdAt;
    public static volatile SingularAttribute<PurchaseRequisition, LocalDateTime> updatedAt;
    public static volatile SingularAttribute<PurchaseRequisition, EntityStatus> entityStatus;

    // Relationships
    public static volatile ListAttribute<PurchaseRequisition, PurchaseRequisitionLine> lines;
    public static volatile ListAttribute<PurchaseRequisition, PurchaseRequisitionAmendment> amendments;
}
