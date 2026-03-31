package projectlx.co.zw.shared_library.model.clients;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class UserAddressDetails {
    private String streetAddress; // user's street address
    private String city; // User's city
    private String state; // user's state
    private String postalCode; // User's postal code
    private String country; // User's country
}
