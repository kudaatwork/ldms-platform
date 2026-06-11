package projectlx.inventory.management.utils.requests;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.inventory.management.model.FreightTerms;
import projectlx.inventory.management.model.ShipMode;
import projectlx.co.zw.shared_library.model.PaymentTerm;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@ToString
public class CreatePOFromPurchaseRequisitionRequest {

    private Long purchaseRequisitionId;
    @JsonAlias("customerId")
    private Long organizationId;
    @JsonAlias("externalId")
    private String externalId;

    // === SUPPLIER INFORMATION ===
    private Long supplierId;
    private String supplierContact;
    private String buyerContact;

    // === FINANCIAL TERMS ===
    private String currency;
    private PaymentTerm paymentTerm;
    private LocalDate paymentDueDate;
    private BigDecimal taxRate;

    // === SHIPPING & LOGISTICS ===
    private Long shipFromLocationId;
    private Long shipToLocationId;
    @JsonAlias("warehouseLocationId")
    private Long receivingWarehouseId;
    private FreightTerms freightTerms;
    private ShipMode shipMode;
    private String shippingInstructions;

    // === ORDER DETAILS ===
    @JsonAlias("expectedDeliveryDate")
    private LocalDate expectedDate;
    private String notes;

    // === LINE SELECTION ===
    // If null, include all eligible lines (fulfillmentMethod=PURCHASE, remainingQty > 0)
    private List<LineSelection> lineSelections;

    // === AUDIT ===
    private Long createdByUserId;

    @Getter
    @Setter
    @ToString
    public static class LineSelection {
        private Long prLineId;
        private BigDecimal quantityToOrder; // Can be less than remaining (partial)
        private BigDecimal unitPrice; // Actual price (may differ from estimate)
    }
}
