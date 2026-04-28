package projectlx.co.zw.locationsmanagementservice.utils.requests;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@RequiredArgsConstructor
public class EditSuburbRequest {

    private Long id; // Identifier field

    // Basic information
    private String name;
    private String code;
    private String postalCode;
    
    // Relationships
    private Long districtId;
    private Long geoCoordinatesId;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Long administrativeLevelId;
}