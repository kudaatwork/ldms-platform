package projectlx.billing.payments.utils.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import projectlx.billing.payments.utils.enums.InvoiceSourceType;
import projectlx.billing.payments.utils.enums.InvoiceStatus;
import projectlx.co.zw.shared_library.model.PaymentTerm;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InvoiceDto {
    private Long id;
    private String invoiceNumber;
    private Long organizationId;
    private Long supplierId;
    private InvoiceSourceType sourceType;
    private Long sourceId;
    private String sourceReference;
    private Long grvId;
    private String grvNumber;
    private Long purchaseOrderId;
    private String purchaseOrderNumber;
    private String transactionCurrencyCode;
    private String baseCurrencyCode;
    private String functionalCurrencyCode;
    private Long exchangeRateSnapshotId;
    private BigDecimal subtotalTransaction;
    private BigDecimal subtotalBase;
    private BigDecimal taxTransaction;
    private BigDecimal taxBase;
    private BigDecimal totalTransaction;
    private BigDecimal totalBase;
    private PaymentTerm paymentTerm;
    private LocalDate paymentDueDate;
    private InvoiceStatus status;
    private LocalDateTime issuedAt;
    private List<InvoiceLineDto> lines;
}
