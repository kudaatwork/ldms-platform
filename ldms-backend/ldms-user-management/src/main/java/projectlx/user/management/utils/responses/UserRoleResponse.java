package projectlx.user.management.utils.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import projectlx.co.zw.shared_library.utils.responses.CommonResponse;
import projectlx.user.management.utils.dtos.UserRoleDto;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.domain.Page;
import java.util.List;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserRoleResponse extends CommonResponse {
    private UserRoleDto userRoleDto;
    private List<UserRoleDto> userRoleDtoList;
    private Page<UserRoleDto> userRoleDtoPage;
    private List<String> errorMessages;
}
