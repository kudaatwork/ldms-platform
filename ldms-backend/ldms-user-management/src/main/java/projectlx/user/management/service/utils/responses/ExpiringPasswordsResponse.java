package projectlx.user.management.service.utils.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.shared_library.utils.responses.CommonResponse;
import projectlx.user.management.service.utils.dtos.ExpiringPasswordDto;

import java.util.List;

/**
 * Response object for expiring passwords
 */
@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExpiringPasswordsResponse extends CommonResponse {
    private List<ExpiringPasswordDto> expiringPasswords;
}