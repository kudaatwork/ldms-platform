package projectlx.co.zw.locationsmanagementservice.utils.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.domain.Page;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.ProvinceDto;
import projectlx.co.zw.shared_library.utils.responses.CommonResponse;
import java.util.List;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProvinceResponse extends CommonResponse {
    private ProvinceDto provinceDto;
    private List<ProvinceDto> provinceDtoList;
    private Page<ProvinceDto> provinceDtoPage;
}