package projectlx.billing.payments.utils.requests;

import lombok.Getter;
import lombok.Setter;
import projectlx.billing.payments.utils.enums.GatewayProvider;
import projectlx.billing.payments.utils.enums.PaymentProofSource;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class CreatePaymentRequest {
    private Long invoiceId;
    private BigDecimal amountTransaction;
    private String paymentMethod;
    private LocalDate paymentDate;
    private String notes;
    private String paymentReferenceNumber;
    private Long proofDocumentId;
    private PaymentProofSource proofSource;
    private GatewayProvider gatewayProvider;
}
