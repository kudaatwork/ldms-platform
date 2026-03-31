package projectlx.user.management.service.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Request object for updating user addresses.
 * Contains both the reference to the user address in the User Management Service
 * and the details to update in the Location Service.
 */
@Getter
@Setter
@ToString
public class EditAddressRequest {
    // User Management Service fields
    private Long id; // ID of the UserAddress entity
    private Long locationAddressId; // ID of the address in the Location Service
    
    // Location Service address fields
    private String line1; // Street address
    private String line2; // Additional address info
    private String postalCode; // Postal code
    private Long suburbId; // ID of the suburb in the Location Service
    private Long geoCoordinatesId; // ID of the geo coordinates in the Location Service
}
