package projectlx.user.management.service.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class EditUserTypeRequest {
    private Long id;
    private String userTypeName;
    private String description;
}
