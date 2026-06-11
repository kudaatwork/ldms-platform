package projectlx.inventory.management.utils.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import projectlx.inventory.management.model.PurchaseOrderLine;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class ProcessedReceiptItem {
    private PurchaseOrderLine orderLine;
    private BigDecimal quantityReceived;
}