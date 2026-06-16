package projectlx.fuel.expenses.utils.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;
import projectlx.co.zw.shared_library.utils.responses.CommonResponse;
import projectlx.fuel.expenses.utils.dtos.FuelTelemetryLogDto;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FuelTelemetryLogResponse extends CommonResponse {

    private FuelTelemetryLogDto fuelTelemetryLogDto;
    private List<FuelTelemetryLogDto> fuelTelemetryLogDtoList;
    private long totalElements;
    private int totalPages;
}
