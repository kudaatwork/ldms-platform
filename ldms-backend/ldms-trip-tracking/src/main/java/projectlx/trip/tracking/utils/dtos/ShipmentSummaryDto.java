package projectlx.trip.tracking.utils.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ShipmentSummaryDto {

    private Long id;
    private String shipmentNumber;
    private Long organizationId;
    private String sourceType;
    private Long inventoryTransferId;
    private Long salesOrderId;
    private String status;
    private String fromWarehouseName;
    private String toWarehouseName;
    private String productName;
    private Boolean crossBorder;
}
