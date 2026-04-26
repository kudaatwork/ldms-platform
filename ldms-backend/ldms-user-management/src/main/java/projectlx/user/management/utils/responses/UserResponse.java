package projectlx.user.management.utils.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import projectlx.co.zw.shared_library.utils.responses.CommonResponse;
import projectlx.user.management.utils.dtos.UserDto;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse extends CommonResponse {
    private UserDto userDto;
    List<UserDto> userDtoList;
    Page<UserDto> userDtoPage;
    private String accessToken;
    private String refreshToken;
    private List<String> errorMessages;
}
