package projectlx.user.management.utils.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.user.management.model.EntityStatus;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserTypeDto {
    private Long id;
    private String userTypeName;
    private String description;
    private EntityStatus entityStatus;
}
