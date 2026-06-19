package projectlx.inventory.management.utils.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.inventory.management.model.CrossDockDispatchStatus;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CrossDockDispatchDto {

    private Long id;
    private Long organizationId;
    private String externalDispatchId;
    private Long productId;
    private String externalProductId;
    private String productCode;
    private BigDecimal quantity;
    private String fromLocationLabel;
    private String toLocationLabel;
    private String customerReference;
    private String enRouteDepotLabels;
    private CrossDockDispatchStatus status;
    private Long shipmentId;
    private String shipmentNumber;
    private Long integrationCredentialId;
    private EntityStatus entityStatus;
    private LocalDateTime createdAt;
    private String createdBy;
}
