package projectlx.user.management.service.utils.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import projectlx.co.zw.shared_library.utils.responses.CommonResponse;
import projectlx.user.management.service.utils.dtos.UserAccountDto;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.domain.Page;
import java.util.List;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserAccountResponse extends CommonResponse {
    private UserAccountDto userAccountDto;
    private List<UserAccountDto> userAccountDtoList;
    private Page<UserAccountDto> userAccountDtoPage;
}
