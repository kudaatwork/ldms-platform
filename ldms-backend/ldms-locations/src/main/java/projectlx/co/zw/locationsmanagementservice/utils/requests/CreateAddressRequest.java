package projectlx.co.zw.locationsmanagementservice.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class CreateAddressRequest {

    // Basic information
    private String line1;
    private String line2;
    private String postalCode;
    
    // Relationships
    private Long suburbId;
    private Long geoCoordinatesId;
}