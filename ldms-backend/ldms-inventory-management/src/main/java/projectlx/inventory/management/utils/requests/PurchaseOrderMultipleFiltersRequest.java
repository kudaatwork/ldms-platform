package projectlx.inventory.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.inventory.management.model.FreightTerms;
import projectlx.inventory.management.model.PurchaseOrderStatus;
import projectlx.inventory.management.model.ShipMode;
import projectlx.co.zw.shared_library.model.PaymentTerm;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.requests.MultipleFiltersRequest;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@ToString
public class PurchaseOrderMultipleFiltersRequest extends MultipleFiltersRequest {

    // === BASIC FILTERS ===
    private String purchaseOrderNumber;
    private String externalId;
    private EntityStatus entityStatus;

    // === PARTY FILTERS ===
    private Long organizationId;
    private Long supplierId;

    // === STATUS & DATES ===
    private PurchaseOrderStatus status;
    private LocalDate orderDate;
    private LocalDate orderDateFrom; // Range filter
    private LocalDate orderDateTo;   // Range filter
    private LocalDate expectedDate;
    private LocalDate expectedDateFrom; // Range filter
    private LocalDate expectedDateTo;   // Range filter

    // === FINANCIAL FILTERS ===
    private String currency;
    private PaymentTerm paymentTerm;
    private LocalDate paymentDueDate;
    private LocalDate paymentDueDateFrom; // Range filter
    private LocalDate paymentDueDateTo;   // Range filter

    // Total amount range filters
    private BigDecimal totalAmountFrom;
    private BigDecimal totalAmountTo;

    // Prepayment filter
    private Boolean prepaymentRequired;

    // === SHIPPING FILTERS ===
    private Long receivingWarehouseId;
    private FreightTerms freightTerms;
    private ShipMode shipMode;

    // === IMPORT/EXPORT FILTERS ===
    private Boolean isImport;
    private String portOfEntry;

    // === APPROVAL FILTERS ===
    private Long approvedByUserId;
    private Boolean isApproved; // Convenience filter: true if approvedByUserId is not null

    // === TEXT SEARCH ===
    private String notes;
    private String searchText; // General search across multiple fields
}