package projectlx.user.management.utils.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import projectlx.co.zw.shared_library.utils.responses.CommonResponse;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UsernameAvailabilityResponse extends CommonResponse {
    private Boolean available;
}
