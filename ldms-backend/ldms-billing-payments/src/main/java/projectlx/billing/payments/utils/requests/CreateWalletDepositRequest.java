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
    private String gatewayProvider;
    private String paymentMethod;
    /** WALLET_TOPUP (default) or SUBSCRIPTION. */
    private String purpose;
    /** Required when purpose = SUBSCRIPTION. */
    private Long subscriptionPackageId;
}
