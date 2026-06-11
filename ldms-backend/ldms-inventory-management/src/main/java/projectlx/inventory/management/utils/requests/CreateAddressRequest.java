package projectlx.inventory.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class CreateAddressRequest {
    private String line1;
    private String line2;
    private String postalCode;
    private Long suburbId;
    private Long geoCoordinatesId;
}
