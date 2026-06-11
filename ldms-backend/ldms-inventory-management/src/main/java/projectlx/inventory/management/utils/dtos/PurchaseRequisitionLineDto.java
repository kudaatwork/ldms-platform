package projectlx.inventory.management.utils.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.inventory.management.model.FulfillmentMethod;
import projectlx.inventory.management.model.UnitOfMeasure;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PurchaseRequisitionLineDto {

    private Long id;
    private Long purchaseRequisitionId;
    private Integer lineNumber;
    private Long productId;
    private String productDescription;
    private UnitOfMeasure unitOfMeasure;

    // Quantity tracking
    private BigDecimal requestedQuantity;
    private BigDecimal approvedQuantity;
    private BigDecimal orderedQuantity;
    private BigDecimal fulfilledFromStockQuantity;
    private BigDecimal fulfilledFromTransferQuantity;
    private BigDecimal remainingQuantity;

    // Pricing estimates
    private BigDecimal estimatedUnitPrice;
    private BigDecimal estimatedTotalPrice;

    // Fulfillment strategy
    private FulfillmentMethod fulfillmentMethod;
    private String fulfillmentNotes;

    // Specifications & requirements
    private String specifications;
    private String preferredBrand;
    private Boolean isSubstituteAcceptable;

    // Approval adjustments
    private String quantityAdjustmentReason;

    // Audit fields
    private Long createdByUserId;
    private Long updatedByUserId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private EntityStatus entityStatus;

    // Product details (enriched)
    private String productName;
    private String productCode;
}
