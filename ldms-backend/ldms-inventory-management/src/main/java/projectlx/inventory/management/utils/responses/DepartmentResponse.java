package projectlx.inventory.management.utils.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.domain.Page;
import projectlx.co.zw.shared_library.utils.responses.CommonResponse;
import projectlx.inventory.management.utils.dtos.DepartmentDto;

import java.util.List;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DepartmentResponse extends CommonResponse {

    private DepartmentDto departmentDto;
    private List<DepartmentDto> departmentDtoList;
    private Page<DepartmentDto> departmentDtoPage;
}
