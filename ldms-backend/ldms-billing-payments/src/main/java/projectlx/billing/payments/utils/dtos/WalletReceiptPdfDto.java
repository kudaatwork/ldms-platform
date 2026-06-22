package projectlx.billing.payments.utils.dtos;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class WalletReceiptPdfDto {
    private final byte[] pdfBytes;
    private final String receiptNumber;
}
