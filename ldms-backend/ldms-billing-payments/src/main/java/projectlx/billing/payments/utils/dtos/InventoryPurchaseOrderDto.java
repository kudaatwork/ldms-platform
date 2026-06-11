package projectlx.billing.payments.utils.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import projectlx.co.zw.shared_library.model.PaymentTerm;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InventoryPurchaseOrderDto {
    private Long id;
    private String purchaseOrderNumber;
    private Long organizationId;
    private Long supplierId;
    private String currency;
    private String functionalCurrencyCode;
    private Long exchangeRateSnapshotId;
    private BigDecimal exchangeRateUsed;
    private PaymentTerm paymentTerm;
    private LocalDate orderDate;
    private LocalDate paymentDueDate;
    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private BigDecimal subtotalFunctional;
    private BigDecimal taxAmountFunctional;
    private BigDecimal totalAmountFunctional;
}
