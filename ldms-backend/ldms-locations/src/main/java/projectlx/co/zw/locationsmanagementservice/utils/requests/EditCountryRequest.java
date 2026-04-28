package projectlx.co.zw.locationsmanagementservice.utils.requests;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@RequiredArgsConstructor
public class EditCountryRequest {

    private Long id; // Identifier field

    // Basic information
    private String name;
    private String isoAlpha2Code;
    private String isoAlpha3Code;
    private String dialCode;
    private String timezone;
    
    // Additional information
    private String currencyCode;
    private Long geoCoordinatesId;
    private BigDecimal latitude;
    private BigDecimal longitude;
}