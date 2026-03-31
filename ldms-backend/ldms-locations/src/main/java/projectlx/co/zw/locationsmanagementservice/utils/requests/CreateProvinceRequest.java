package projectlx.co.zw.locationsmanagementservice.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class CreateProvinceRequest {

    // Basic information
    private String name;
    private String code;
    
    // Relationships
    private Long countryId;
    private Long administrativeLevelId;
    private Long geoCoordinatesId;
}