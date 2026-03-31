package projectlx.co.zw.locationsmanagementservice.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class CreateSuburbRequest {

    // Basic information
    private String name;
    private String code;
    private String postalCode;
    
    // Relationships
    private Long districtId;
    private Long geoCoordinatesId;
    private Long administrativeLevelId;
}