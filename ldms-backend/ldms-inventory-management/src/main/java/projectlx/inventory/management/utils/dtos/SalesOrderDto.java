package projectlx.inventory.management.utils.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.inventory.management.model.SalesOrderStatus;
import projectlx.co.zw.shared_library.model.PaymentTerm;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SalesOrderDto {

    private Long id;
    private String salesOrderNumber;
    private Long purchaseOrderId;
    private String purchaseOrderNumber;
    private Long customerId;
    private Long supplierOrganizationId;
    private SalesOrderStatus status;
    private Integer currentApprovalStage;
    private Integer requiredApprovalStages;
    private LocalDate orderDate;
    private LocalDate expectedDeliveryDate;
    private LocalDateTime deliveredDate;
    private BigDecimal totalAmount;
    private PaymentTerm paymentTerm;
    private String notes;
    private Long createdByUserId;
    private Long updatedByUserId;
    private Long warehouseLocationId;

    private List<SalesOrderLineDto> salesOrderLines;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private EntityStatus entityStatus;
}
