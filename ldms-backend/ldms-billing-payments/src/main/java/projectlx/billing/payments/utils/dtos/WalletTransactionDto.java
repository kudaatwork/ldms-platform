package projectlx.billing.payments.utils.dtos;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class WalletTransactionDto {
    private Long id;
    private String transactionType;
    private Long amountCents;
    private Long balanceAfterCents;
    private String actionCode;
    private String description;
    private String receiptNumber;
    private Long receiptDocumentId;
    private Long tripId;
    private Long seasonId;
    private String createdAt;
}
