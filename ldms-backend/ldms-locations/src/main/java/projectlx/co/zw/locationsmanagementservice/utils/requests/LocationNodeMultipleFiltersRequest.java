package projectlx.co.zw.locationsmanagementservice.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.locationsmanagementservice.utils.enums.LocationType;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

@Getter
@Setter
@ToString
public class LocationNodeMultipleFiltersRequest {
    private String searchValue;
    private String name;
    private String code;
    private String timezone;
    private String parentName;
    private LocationType locationType;
    private Long parentId;
    private EntityStatus entityStatus;
    private int page = 0;
    private int size = 20;
}
