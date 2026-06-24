package projectlx.billing.payments.utils.dtos;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class WalletDepositDto {
    private Long id;
    private Long organizationId;
    private Long amountCents;
    private String currencyCode;
    private String referenceNumber;
    private String notes;
    private String status;
    private Long proofDocumentId;
    private String gatewayProvider;
    private String paymentMethod;
    private String rejectionReason;
    private String createdAt;
    private String modifiedAt;
    private String modifiedBy;
}
