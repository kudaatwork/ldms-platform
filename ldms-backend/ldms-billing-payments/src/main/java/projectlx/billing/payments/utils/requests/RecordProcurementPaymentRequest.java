package projectlx.billing.payments.utils.requests;

import lombok.Getter;
import lombok.Setter;
import projectlx.billing.payments.utils.enums.PaymentProofSource;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class RecordProcurementPaymentRequest {
    private Long purchaseOrderId;
    private BigDecimal amount;
    private String currency;
    private String paymentMethod;
    private String referenceNumber;
    private String notes;
    private LocalDate paymentDate;
    private PaymentProofSource proofSource;
    /** Pre-uploaded file id when proofSource is EXTERNAL_UPLOAD. */
    private Long proofDocumentId;
}
