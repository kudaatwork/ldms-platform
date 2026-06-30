package projectlx.inventory.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class EditDepartmentRequest {

    private Long departmentId;
    private String name;
    private String departmentCode;
    private String description;
}
