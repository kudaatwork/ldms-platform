package projectlx.inventory.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.inventory.management.model.FulfillmentMethod;
import projectlx.inventory.management.model.PriorityLevel;
import projectlx.inventory.management.model.UnitOfMeasure;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@ToString
public class CreatePurchaseRequisitionRequest {

    // === REQUESTER INFORMATION ===
    private Long organizationId;
    private Long departmentId;
    private Long requestedByUserId;
    private String costCenter;
    private String projectCode;

    // === PURPOSE & JUSTIFICATION ===
    private String purpose;
    private String justification;
    private PriorityLevel priority;

    // === DATES ===
    private LocalDate requisitionDate;
    private LocalDate requiredByDate;
    private LocalDate expiryDate;

    // === FULFILLMENT STRATEGY ===
    private FulfillmentMethod defaultFulfillmentMethod;
    private Long targetWarehouseId;
    private Long preferredSupplierId;

    // === FINANCIAL ESTIMATES ===
    private String currency;
    private Boolean budgetAvailable;
    private String budgetCode;

    // === NOTES ===
    private String notes;

    // === LINE ITEMS ===
    private List<PurchaseRequisitionLineRequest> lines;

    // === AUDIT ===
    private Long createdByUserId;

    @Getter
    @Setter
    @ToString
    public static class PurchaseRequisitionLineRequest {
        private Long productId;
        private String productDescription;
        private UnitOfMeasure unitOfMeasure;
        private BigDecimal requestedQuantity;
        private BigDecimal estimatedUnitPrice;
        private FulfillmentMethod fulfillmentMethod;
        private String specifications;
        private String preferredBrand;
        private Boolean isSubstituteAcceptable;
    }
}
