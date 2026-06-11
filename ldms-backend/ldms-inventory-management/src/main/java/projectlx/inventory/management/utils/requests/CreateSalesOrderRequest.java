package projectlx.inventory.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.inventory.management.model.SalesOrderStatus;
import projectlx.inventory.management.model.UnitOfMeasure;
import projectlx.co.zw.shared_library.model.PaymentTerm;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@ToString
public class CreateSalesOrderRequest {

    private Long customerId;
    private SalesOrderStatus status;
    private LocalDate orderDate;
    private LocalDate expectedDeliveryDate;
    private String notes;
    private Long createdByUserId;
    private PaymentTerm paymentTerm;

    private List<SalesOrderLineRequest> lines;

    @Getter
    @Setter
    @ToString
    public static class SalesOrderLineRequest {
        private Long productId;
        private BigDecimal quantity;
        private BigDecimal unitPrice;
        private UnitOfMeasure unitOfMeasure;
    }
}
