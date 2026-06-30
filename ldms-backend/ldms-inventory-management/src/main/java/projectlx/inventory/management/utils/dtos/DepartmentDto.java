package projectlx.inventory.management.utils.dtos;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class DepartmentDto {

    private Long id;
    private String name;
    private String departmentCode;
    private String description;
    private Long supplierId;

    /** True when at least one purchase requisition references this department. */
    private Boolean inUse;
}
