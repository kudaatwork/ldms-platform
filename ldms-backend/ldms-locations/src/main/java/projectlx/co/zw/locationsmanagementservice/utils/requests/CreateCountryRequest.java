package projectlx.co.zw.locationsmanagementservice.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class CreateCountryRequest {

    // Basic information
    private String name;
    private String isoAlpha2Code;
    private String isoAlpha3Code;
    private String dialCode;
    private String timezone;
    
    // Additional information
    private String currencyCode;
    private Long geoCoordinatesId;
}