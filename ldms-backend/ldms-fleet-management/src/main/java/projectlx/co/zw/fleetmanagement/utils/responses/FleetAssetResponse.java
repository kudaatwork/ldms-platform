package projectlx.co.zw.fleetmanagement.utils.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.fleetmanagement.utils.dtos.FleetAssetDto;
import projectlx.co.zw.shared_library.utils.responses.CommonResponse;

import java.util.List;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FleetAssetResponse extends CommonResponse {
    private FleetAssetDto fleetAssetDto;
    private List<FleetAssetDto> fleetAssetDtoList;
}
