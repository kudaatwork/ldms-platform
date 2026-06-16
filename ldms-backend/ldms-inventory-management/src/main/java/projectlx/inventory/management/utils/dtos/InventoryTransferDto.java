package projectlx.inventory.management.utils.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.inventory.management.model.TransferStatus;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InventoryTransferDto {

    private Long id;

    private String transferNumber;
    private Long productId;
    private String productName;
    private String productCode;
    private String unitOfMeasure;
    private Long fromLocationId;
    private String fromWarehouseName;
    private Long toLocationId;
    private String toWarehouseName;
    private BigDecimal quantity;
    private BigDecimal unitCost;
    private TransferStatus status;
    private String reference;
    private Boolean crossBorder;
    private Long shipmentId;
    private String rejectionReason;
    private Long rejectedByUserId;
    private LocalDateTime rejectedAt;

    private Long createdByUserId;
    private String requestedBy;
    private Long updatedByUserId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private EntityStatus entityStatus;

    private Boolean canApprove;
    private Boolean canStartTransit;
    private Boolean canComplete;
    private Boolean canCancel;
}