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
public class EditPurchaseRequisitionRequest {

    private Long id;

    // === REQUESTER INFORMATION ===
    private Long departmentId;
    private String costCenter;
    private String projectCode;

    // === PURPOSE & JUSTIFICATION ===
    private String purpose;
    private String justification;
    private PriorityLevel priority;

    // === DATES ===
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
    private List<EditPurchaseRequisitionLineRequest> lines;

    // === AUDIT ===
    private Long updatedByUserId;

    @Getter
    @Setter
    @ToString
    public static class EditPurchaseRequisitionLineRequest {
        private Long id; // Null for new lines
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
