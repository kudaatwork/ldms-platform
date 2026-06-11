package projectlx.inventory.management.utils.dtos;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class GoodsReceiptResult {
    private boolean success;
    private List<ProcessedReceiptItem> processedItems;
    private List<ReceiptValidationError> errors;
    
    public static GoodsReceiptResult success(List<ProcessedReceiptItem> items) {
        return GoodsReceiptResult.builder()
            .success(true)
            .processedItems(items)
            .build();
    }
    
    public static GoodsReceiptResult failure(List<ReceiptValidationError> errors) {
        return GoodsReceiptResult.builder()
            .success(false)
            .errors(errors)
            .build();
    }
}