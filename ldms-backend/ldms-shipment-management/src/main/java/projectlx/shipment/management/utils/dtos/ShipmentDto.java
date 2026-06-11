package projectlx.shipment.management.utils.dtos;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
public class ShipmentDto {

    private Long id;
    private String shipmentNumber;
    private Long organizationId;
    private String sourceType;
    private Long inventoryTransferId;
    private Long fromWarehouseLocationId;
    private Long toWarehouseLocationId;
    private String fromWarehouseName;
    private String toWarehouseName;
    private Long productId;
    private String productName;
    private String productCode;
    private BigDecimal quantity;
    private Long fleetDriverId;
    private Long fleetAssetId;
    private Long tripId;
    private String status;
    private String notes;
    private String entityStatus;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime modifiedAt;
    private String modifiedBy;
}
