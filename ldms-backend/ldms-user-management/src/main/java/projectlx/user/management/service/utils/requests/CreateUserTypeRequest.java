package projectlx.user.management.service.utils.requests;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateUserTypeRequest {
    private String userTypeName;
    private String description;
}
