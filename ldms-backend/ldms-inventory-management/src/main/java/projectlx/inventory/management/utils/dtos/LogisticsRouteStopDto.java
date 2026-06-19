package projectlx.inventory.management.utils.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.inventory.management.model.RouteStopContextType;
import projectlx.inventory.management.model.RouteStopType;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LogisticsRouteStopDto {

    private Long id;
    private Long organizationId;
    private RouteStopContextType contextType;
    private Long contextId;
    private Integer stopSequence;
    private RouteStopType stopType;
    private Long warehouseLocationId;
    private Long branchId;
    private String locationLabel;
    private EntityStatus entityStatus;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime modifiedAt;
    private String modifiedBy;
}
