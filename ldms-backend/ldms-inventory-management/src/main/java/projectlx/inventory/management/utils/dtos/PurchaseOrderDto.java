package projectlx.inventory.management.utils.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.inventory.management.model.PurchaseOrderStatus;
import projectlx.co.zw.shared_library.model.PaymentTerm;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PurchaseOrderDto {

    private Long id;

    // New fields aligned with adjusted PurchaseOrder entity
    private String purchaseOrderNumber;
    private String externalId;
    private Long organizationId;
    private Long supplierId;
    private String currency;
    private String functionalCurrencyCode;
    private Long exchangeRateSnapshotId;
    private BigDecimal exchangeRateUsed;
    private PaymentTerm paymentTerm;
    private LocalDate paymentDueDate;
    private BigDecimal subtotal;
    private BigDecimal taxRate;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private BigDecimal subtotalFunctional;
    private BigDecimal taxAmountFunctional;
    private BigDecimal totalAmountFunctional;
    private Long createdByUserId;
    private Long updatedByUserId;

    private PurchaseOrderStatus status;
    private Integer currentCustomerApprovalStage;
    private Integer currentSupplierApprovalStage;
    private Integer requiredApprovalStages;
    private Boolean customerApprovalComplete;
    private Boolean supplierApprovalComplete;
    private LocalDate orderDate;
    private LocalDate expectedDate;
    private LocalDateTime receivedDate;

    private String notes;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private EntityStatus entityStatus;
}
