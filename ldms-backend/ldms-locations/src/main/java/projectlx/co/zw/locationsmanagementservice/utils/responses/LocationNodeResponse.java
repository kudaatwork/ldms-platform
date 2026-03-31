package projectlx.co.zw.locationsmanagementservice.utils.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.domain.Page;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.LocationNodeDto;
import projectlx.co.zw.shared_library.utils.responses.CommonResponse;

import java.util.List;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LocationNodeResponse extends CommonResponse {
    private LocationNodeDto locationNodeDto;
    private List<LocationNodeDto> locationNodeDtoList;
    private Page<LocationNodeDto> locationNodeDtoPage;
}
