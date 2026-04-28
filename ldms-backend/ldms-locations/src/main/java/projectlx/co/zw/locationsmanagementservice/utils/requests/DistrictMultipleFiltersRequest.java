package projectlx.co.zw.locationsmanagementservice.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.shared_library.utils.requests.MultipleFiltersRequest;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

@Getter
@Setter
@ToString
public class DistrictMultipleFiltersRequest extends MultipleFiltersRequest {
    private String name;
    private String code;
    private Long provinceId;
    private Long administrativeLevelId;
    private EntityStatus entityStatus;
}