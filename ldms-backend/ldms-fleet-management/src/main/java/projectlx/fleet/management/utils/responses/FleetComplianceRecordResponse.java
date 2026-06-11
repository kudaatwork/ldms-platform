package projectlx.fleet.management.utils.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.fleet.management.utils.dtos.FleetComplianceRecordDto;
import projectlx.co.zw.shared_library.utils.responses.CommonResponse;

import java.util.List;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FleetComplianceRecordResponse extends CommonResponse {
    private FleetComplianceRecordDto fleetComplianceRecordDto;
    private List<FleetComplianceRecordDto> fleetComplianceRecordDtoList;
}
