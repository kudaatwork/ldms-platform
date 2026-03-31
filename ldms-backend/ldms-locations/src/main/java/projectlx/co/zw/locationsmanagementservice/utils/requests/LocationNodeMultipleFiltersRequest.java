package projectlx.co.zw.locationsmanagementservice.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.locationsmanagementservice.utils.enums.LocationType;

@Getter
@Setter
@ToString
public class LocationNodeMultipleFiltersRequest {
    private String searchValue;
    private LocationType locationType;
    private Long parentId;
    private int page = 0;
    private int size = 20;
}
