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
    /** Organisation workspace login names (lightweight listing for audit scoping). */
    private List<String> usernames;
    Page<UserDto> userDtoPage;
    private String accessToken;
    private String refreshToken;
    private List<String> errorMessages;
    /** Set by verify-email only: {@code VERIFIED} or {@code ALREADY_VERIFIED}. */
    private String emailVerificationOutcome;
    /** Plain-text temporary username returned once when credentials are issued after KYC approval. */
    private String temporaryUsername;
    /** Plain-text temporary password returned once when credentials are issued after KYC approval. */
    private String temporaryPassword;
}
