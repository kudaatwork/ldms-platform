package projectlx.inventory.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.inventory.management.model.SalesOrderStatus;
import projectlx.inventory.management.model.UnitOfMeasure;
import projectlx.co.zw.shared_library.model.PaymentTerm;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@ToString
public class EditSalesOrderRequest {

    private Long salesOrderId;
    private Long customerId;
    private SalesOrderStatus status;
    private LocalDate orderDate;
    private LocalDate expectedDeliveryDate;
    private String notes;
    private Long updatedByUserId;
    private EntityStatus entityStatus;
    private PaymentTerm paymentTerm;

    private List<SalesOrderLineUpdateRequest> lines;

    @Getter
    @Setter
    @ToString
    public static class SalesOrderLineUpdateRequest {
        private Long salesOrderLineId; // null for new lines
        private Long productId;
        private BigDecimal quantity;
        private BigDecimal unitPrice;
        private UnitOfMeasure unitOfMeasure;
    }
}
