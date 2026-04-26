package projectlx.user.management.utils.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import projectlx.co.zw.shared_library.utils.responses.CommonResponse;
import projectlx.user.management.utils.dtos.UserGroupDto;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.domain.Page;
import java.util.List;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserGroupResponse extends CommonResponse {
    private UserGroupDto userGroupDto;
    private List<UserGroupDto> userGroupDtoList;
    private Page<UserGroupDto> userGroupDtoPage;
    private List<String> errorMessages;
}
