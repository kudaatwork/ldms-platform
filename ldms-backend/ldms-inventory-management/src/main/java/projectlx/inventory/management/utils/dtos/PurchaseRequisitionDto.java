package projectlx.inventory.management.utils.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.inventory.management.model.FulfillmentMethod;
import projectlx.inventory.management.model.PriorityLevel;
import projectlx.inventory.management.model.PurchaseRequisitionStatus;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PurchaseRequisitionDto {

    private Long id;
    private String requisitionNumber;
    private String externalReference;
    private Integer version;

    // Requester information
    private Long organizationId;
    private Long departmentId;
    private Long requestedByUserId;
    private String costCenter;
    private String projectCode;

    // Purpose & justification
    private String purpose;
    private String justification;
    private PriorityLevel priority;

    // Dates
    private LocalDate requisitionDate;
    private LocalDate requiredByDate;
    private LocalDate expiryDate;

    // Status & workflow
    private PurchaseRequisitionStatus status;
    private Integer currentApprovalStage;
    private Integer requiredApprovalStages;

    // Submission tracking
    private LocalDateTime submittedAt;
    private Long submittedByUserId;

    // Approval tracking
    private Long approvedByUserId;
    private LocalDateTime approvedAt;
    private String approvalNotes;
    private LocalDateTime publishedToSupplierAt;
    private Long supplierQuoteId;
    private LocalDateTime acknowledgedAt;
    private Long acknowledgedByUserId;

    // Rejection tracking
    private Long rejectedByUserId;
    private LocalDateTime rejectedAt;
    private String rejectionReason;

    // Cancellation tracking
    private Long cancelledByUserId;
    private LocalDateTime cancelledAt;
    private String cancellationReason;

    // Fulfillment strategy
    private FulfillmentMethod defaultFulfillmentMethod;
    private Long targetWarehouseId;
    private Long preferredSupplierId;

    // Financial estimates
    private BigDecimal estimatedTotal;
    private String currency;
    private Boolean budgetAvailable;
    private String budgetCode;

    // Audit fields
    private Long createdByUserId;
    private Long updatedByUserId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private EntityStatus entityStatus;

    // Notes
    private String notes;

    // Relationships
    private List<PurchaseRequisitionLineDto> lines;

    // Summary fields (computed)
    private Integer totalLines;
    private Integer fulfilledLines;
    private Integer partiallyFulfilledLines;
    private Integer pendingLines;
}
