package projectlx.billing.payments.utils.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import projectlx.billing.payments.utils.enums.GatewayProvider;
import projectlx.billing.payments.utils.enums.PaymentRecordStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentDto {
    private Long id;
    private String paymentReference;
    private Long invoiceId;
    private Long organizationId;
    private String transactionCurrencyCode;
    private String baseCurrencyCode;
    private String functionalCurrencyCode;
    private Long exchangeRateSnapshotId;
    private Long invoiceExchangeRateSnapshotId;
    private BigDecimal amountTransaction;
    private BigDecimal amountBase;
    private BigDecimal amountFunctionalAtOrigination;
    private BigDecimal amountFunctionalAtSettlement;
    private BigDecimal realizedFxGainLoss;
    private String paymentMethod;
    private LocalDate paymentDate;
    private PaymentRecordStatus status;
    private String notes;
    private String paymentReferenceNumber;
    private Long proofDocumentId;
    private String proofSource;
    private GatewayProvider gatewayProvider;
    private LocalDateTime verifiedAt;
    private String verifiedBy;
    private Integer currentVerificationStage;
    private Integer requiredVerificationStages;
    private String invoiceNumber;
    private Long purchaseOrderId;
    private String purchaseOrderNumber;
}
