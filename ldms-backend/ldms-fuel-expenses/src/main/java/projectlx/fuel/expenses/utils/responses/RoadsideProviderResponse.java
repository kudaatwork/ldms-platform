package projectlx.fuel.expenses.utils.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import projectlx.co.zw.shared_library.utils.responses.CommonResponse;
import projectlx.fuel.expenses.utils.dtos.RoadsideProviderDto;

import java.util.List;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoadsideProviderResponse extends CommonResponse {
    private RoadsideProviderDto roadsideProviderDto;
    private List<RoadsideProviderDto> roadsideProviderDtoList;
}
