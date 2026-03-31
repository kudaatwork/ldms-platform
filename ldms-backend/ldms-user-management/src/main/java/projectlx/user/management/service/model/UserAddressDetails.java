package projectlx.user.management.service.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class UserAddressDetails {
    private String line1; // Street address
    private String line2; // Additional address info
    private String postalCode; // Postal code
    private Long suburbId; // ID of the suburb in the Location Service
    private Long geoCoordinatesId; // ID of the geo coordinates in the Location Service
}
