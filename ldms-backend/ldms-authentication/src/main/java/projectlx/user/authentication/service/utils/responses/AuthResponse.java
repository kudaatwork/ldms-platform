package projectlx.user.authentication.service.utils.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.domain.Page;
import projectlx.co.zw.shared_library.utils.responses.CommonResponse;
import projectlx.user.authentication.service.utils.dtos.AuthDto;
import java.util.List;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse extends CommonResponse {
    private AuthDto authDto;
    private List<AuthDto> authDtoList;
    private Page<AuthDto> authDtoPage;
    private String accessToken;
    private String refreshToken;
}
