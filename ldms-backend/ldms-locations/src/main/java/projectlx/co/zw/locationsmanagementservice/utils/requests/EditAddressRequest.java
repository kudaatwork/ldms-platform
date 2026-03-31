package projectlx.co.zw.locationsmanagementservice.utils.requests;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class EditAddressRequest {

    private Long id; // Identifier field

    // Basic information
    private String line1;
    private String line2;
    private String postalCode;
    
    // Relationships
    private Long suburbId;
    private Long geoCoordinatesId;
}