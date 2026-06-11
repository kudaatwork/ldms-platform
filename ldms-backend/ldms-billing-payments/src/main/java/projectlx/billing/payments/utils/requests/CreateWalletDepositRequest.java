package projectlx.billing.payments.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class CreateWalletDepositRequest {
    private Long amountCents;
    private String currencyCode;
    private String referenceNumber;
    private String notes;
    private Long proofDocumentId;
}
