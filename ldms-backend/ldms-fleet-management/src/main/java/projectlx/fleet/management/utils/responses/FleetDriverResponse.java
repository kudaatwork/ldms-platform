package projectlx.fleet.management.utils.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.fleet.management.utils.dtos.FleetDriverDto;
import projectlx.co.zw.shared_library.utils.responses.CommonResponse;

import java.util.List;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FleetDriverResponse extends CommonResponse {
    private FleetDriverDto fleetDriverDto;
    private List<FleetDriverDto> fleetDriverDtoList;
    private List<String> errorMessages;
    /** Plain-text credentials returned once when platform access is provisioned. */
    private String temporaryUsername;
    private String temporaryPassword;
}
