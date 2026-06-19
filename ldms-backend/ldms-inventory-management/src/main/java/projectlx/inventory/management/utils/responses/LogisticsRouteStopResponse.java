package projectlx.inventory.management.utils.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.inventory.management.utils.dtos.LogisticsRouteStopDto;
import projectlx.co.zw.shared_library.utils.responses.CommonResponse;

import java.util.List;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LogisticsRouteStopResponse extends CommonResponse {
    private LogisticsRouteStopDto logisticsRouteStopDto;
    private List<LogisticsRouteStopDto> logisticsRouteStopDtoList;
}
