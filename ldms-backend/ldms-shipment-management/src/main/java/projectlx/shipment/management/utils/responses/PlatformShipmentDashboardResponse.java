package projectlx.shipment.management.utils.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.shared_library.utils.responses.CommonResponse;
import projectlx.shipment.management.utils.dtos.PlatformShipmentDashboardDto;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlatformShipmentDashboardResponse extends CommonResponse {

    private PlatformShipmentDashboardDto platformShipmentDashboardDto;
}
