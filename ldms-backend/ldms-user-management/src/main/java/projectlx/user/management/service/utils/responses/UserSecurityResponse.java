package projectlx.user.management.service.utils.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import projectlx.co.zw.shared_library.utils.responses.CommonResponse;
import projectlx.user.management.service.utils.dtos.UserSecurityDto;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.domain.Page;
import java.util.List;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserSecurityResponse extends CommonResponse {
    private UserSecurityDto userSecurityDto;
    private List<UserSecurityDto> userSecurityDtoList;
    private Page<UserSecurityDto> userSecurityDtoPage;
    private List<String> errorMessages;
}
