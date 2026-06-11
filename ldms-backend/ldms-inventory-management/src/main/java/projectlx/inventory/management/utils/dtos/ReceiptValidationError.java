package projectlx.inventory.management.utils.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ReceiptValidationError {
    private Long purchaseOrderLineId;
    private String errorMessage;
}