package projectlx.fleet.management.utils.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.shared_library.utils.responses.CommonResponse;
import projectlx.fleet.management.utils.dtos.FleetTrackingIntegrationCredentialDto;

import java.util.List;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FleetTrackingIntegrationCredentialResponse extends CommonResponse {
    private FleetTrackingIntegrationCredentialDto fleetTrackingIntegrationCredentialDto;
    private List<FleetTrackingIntegrationCredentialDto> fleetTrackingIntegrationCredentialDtoList;
}
