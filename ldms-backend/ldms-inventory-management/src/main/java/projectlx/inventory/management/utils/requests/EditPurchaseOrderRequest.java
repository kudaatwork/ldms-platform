package projectlx.inventory.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.inventory.management.model.FreightTerms;
import projectlx.inventory.management.model.PurchaseOrderStatus;
import projectlx.inventory.management.model.ShipMode;
import projectlx.inventory.management.model.UnitOfMeasure;
import projectlx.co.zw.shared_library.model.PaymentTerm;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@ToString
public class EditPurchaseOrderRequest {

    // === IDENTIFIER ===
    private Long purchaseOrderId;

    // === PARTY INFORMATION ===
    // Organization making the purchase
    private Long organizationId;

    // Editable supplier reference
    private Long supplierId;

    // Contact information for both parties
    private String buyerContact;
    private String supplierContact;

    // === FINANCIAL TERMS ===
    // Currency for the purchase order (ISO 4217 code)
    private String currency;

    // Payment terms and due date
    private PaymentTerm paymentTerm;
    private LocalDate paymentDueDate;

    // Tax rate (e.g., 15.00 for 15% VAT)
    private BigDecimal taxRate;

    // Optional early payment incentive
    private BigDecimal earlyPaymentDiscountPct;
    private LocalDate earlyPaymentDiscountUntil;

    // Optional prepayment requirement
    private Boolean prepaymentRequired;
    private BigDecimal prepaymentPercent;

    // === SHIPPING & LOGISTICS ===
    // Shipping locations
    private Long shipFromLocationId;
    private Long shipToLocationId;
    private Long receivingWarehouseId; // Where inventory will be received

    // Freight terms (Incoterms)
    private FreightTerms freightTerms; // FOB, CIF, EXW, DDP

    // Shipping mode
    private ShipMode shipMode; // ROAD, SEA, AIR, RAIL

    // Special shipping instructions
    private String shippingInstructions;

    // === IMPORT/EXPORT ===
    // Flag for import orders
    private Boolean isImport;

    // Customs information
    private String customsDeclarationNumber;
    private String portOfEntry;

    // === ORDER DETAILS ===
    // Editable PO number (if allowed by business rules)
    private String purchaseOrderNumber;

    // External reference
    private String externalId;

    // Order status and dates
    private PurchaseOrderStatus status;
    private LocalDate orderDate;
    private LocalDate expectedDate;

    // Notes or comments
    private String notes;

    // Entity status
    private EntityStatus entityStatus;

    // === APPROVAL WORKFLOW ===
    // Approval tracking (set when approving/rejecting)
    private Long approvedByUserId;
    private String approvalNotes;

    // === AUDIT ===
    // User who is updating the order
    private Long updatedByUserId;

    // === LINE ITEMS ===
    // List of line items to be updated or replaced
    private List<PurchaseOrderLineUpdateRequest> lines;

    /**
     * Inner class to represent a line item update request
     */
    @Getter
    @Setter
    @ToString
    public static class PurchaseOrderLineUpdateRequest {

        private Long purchaseOrderLineId;
        private Long productId;
        private UnitOfMeasure unitOfMeasure;
        private BigDecimal quantity;
        private BigDecimal unitPrice;
        private BigDecimal receivedQuantity;
        private EntityStatus entityStatus;
    }
}