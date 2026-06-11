package projectlx.inventory.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.inventory.management.model.ReferenceDocumentType;

import java.math.BigDecimal;
import java.util.Locale;

@Getter
@Setter
@ToString
public class CreateOrUpdateStockRequest {

    private Long productId;
    private Long warehouseLocationId;
    private BigDecimal quantityReceived;
    private String reason;
    private Long referenceDocumentId;
    private Long userId;
    private ReferenceDocumentType referenceDocumentType;
    private BigDecimal unitCost;
    private Long updatedByUserId;
}
