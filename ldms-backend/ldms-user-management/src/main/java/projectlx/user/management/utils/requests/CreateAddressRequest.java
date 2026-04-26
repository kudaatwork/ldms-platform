package projectlx.user.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Request object for creating user addresses.
 * Contains the details to create an address in the Location Service.
 */
@Getter
@Setter
@ToString
public class CreateAddressRequest {
    // Location Service address fields
    private String line1; // Street address
    private String line2; // Additional address info
    private String postalCode; // Postal code
    private Long suburbId; // ID of the suburb in the Location Service
    private Long geoCoordinatesId; // ID of the geo coordinates in the Location Service
}
