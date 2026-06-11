package projectlx.inventory.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.inventory.management.model.FreightTerms;
import projectlx.inventory.management.model.PurchaseOrderStatus;
import projectlx.inventory.management.model.ShipMode;
import projectlx.inventory.management.model.UnitOfMeasure;
import projectlx.co.zw.shared_library.model.PaymentTerm;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@ToString
public class CreatePurchaseOrderRequest {

    // === PARTY INFORMATION ===
    // External reference to the Order Management or Finance Service
    private String externalId;

    // Organization making the purchase
    private Long organizationId;

    // External reference to supplier service
    private Long supplierId;

    // Contact information for both parties
    private String buyerContact;
    private String supplierContact;

    // A reference to the organization's user who created the order
    private Long createdByUserId;

    // === FINANCIAL TERMS ===
    // Currency for the purchase order (ISO 4217 code: USD, ZWL, ZAR, etc.)
    private String currency;

    // Payment terms and due date
    private PaymentTerm paymentTerm;
    private LocalDate paymentDueDate;

    // Tax rate (e.g., 15.00 for 15% VAT)
    private BigDecimal taxRate;

    // Optional early payment incentive
    private BigDecimal earlyPaymentDiscountPct;
    private LocalDate earlyPaymentDiscountUntil;

    // Optional prepayment requirement (common in African markets)
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

    // === IMPORT/EXPORT (for cross-border trade) ===
    // Flag for import orders
    private Boolean isImport;

    // Customs information
    private String customsDeclarationNumber;
    private String portOfEntry;

    // === ORDER DETAILS ===
    // Order status and dates
    private PurchaseOrderStatus status;
    private LocalDate orderDate;
    private LocalDate expectedDate;

    // Notes or comments for the purchase order
    private String notes;

    // === LINE ITEMS ===
    // List of items in the purchase order
    private List<PurchaseOrderLineRequest> lines;

    /**
     * A static inner class to represent a line item within the purchase order request.
     * This DTO is used to capture the details of each product being ordered.
     */
    @Getter
    @Setter
    @ToString
    public static class PurchaseOrderLineRequest {

        // Relationships
        private Long productId;

        // Details
        private UnitOfMeasure unitOfMeasure;

        // Using BigDecimal for quantity to match the entity
        private BigDecimal quantity;
        private BigDecimal unitPrice;
    }
}