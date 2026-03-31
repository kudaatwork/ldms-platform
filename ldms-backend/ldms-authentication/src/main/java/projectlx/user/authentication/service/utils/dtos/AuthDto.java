package projectlx.user.authentication.service.utils.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthDto {
    private Long id;
    private String accessToken;
    private String refreshToken;
    private boolean revoked;
    private boolean expired;
    private String username; // just
}
