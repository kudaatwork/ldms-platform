package projectlx.co.zw.locationsmanagementservice.utils.requests;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class EditProvinceRequest {

    private Long id; // Identifier field

    // Basic information
    private String name;
    private String code;
    
    // Relationships
    private Long countryId;
    private Long administrativeLevelId;
    private Long geoCoordinatesId;
}