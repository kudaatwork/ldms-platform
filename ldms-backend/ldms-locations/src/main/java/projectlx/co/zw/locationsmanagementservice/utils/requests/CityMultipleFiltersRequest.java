package projectlx.co.zw.locationsmanagementservice.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.requests.MultipleFiltersRequest;

@Getter
@Setter
@ToString
public class CityMultipleFiltersRequest extends MultipleFiltersRequest {

    private String name;
    private String code;
    private Long districtId;
    private EntityStatus entityStatus;
}
