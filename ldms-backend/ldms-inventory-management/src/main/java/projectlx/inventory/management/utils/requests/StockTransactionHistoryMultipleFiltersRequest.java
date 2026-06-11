package projectlx.inventory.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.inventory.management.model.ReferenceDocumentType;
import projectlx.inventory.management.model.TransactionType;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.requests.MultipleFiltersRequest;
import java.math.BigDecimal;

@Getter
@Setter
@ToString
public class StockTransactionHistoryMultipleFiltersRequest extends MultipleFiltersRequest {

    private Long inventoryItemId;
    private Long warehouseLocationId;
    private TransactionType transactionType;
    private BigDecimal quantityChange;
    private String reason;
    private Long performedByUserId;
    private Long referenceDocumentId;
    private ReferenceDocumentType referenceDocumentType;
    private EntityStatus entityStatus;
}
