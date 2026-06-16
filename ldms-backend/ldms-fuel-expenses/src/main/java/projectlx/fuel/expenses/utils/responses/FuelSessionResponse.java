package projectlx.fuel.expenses.utils.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;
import projectlx.co.zw.shared_library.utils.responses.CommonResponse;
import projectlx.fuel.expenses.utils.dtos.FuelSessionDto;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FuelSessionResponse extends CommonResponse {

    private FuelSessionDto fuelSessionDto;
    private List<FuelSessionDto> fuelSessionDtoList;
}
