package projectlx.user.management.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@Setter
@ToString
public class UserAddressDetails {
    private String line1; // Street address
    private String line2; // Additional address info
    private String postalCode; // Postal code
    private Long suburbId; // ID of the suburb in the Location Service
    private Long geoCoordinatesId; // ID of the geo coordinates in the Location Service
    /** Optional: when set without geoCoordinatesId, Location Service may create coordinates from lat/long. */
    private BigDecimal latitude;
    private BigDecimal longitude;
}
