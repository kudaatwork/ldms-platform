package projectlx.co.zw.locationsmanagementservice.utils.requests;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import projectlx.co.zw.locationsmanagementservice.utils.enums.SettlementType;

import java.math.BigDecimal;

@Getter
@Setter
@RequiredArgsConstructor
public class EditAddressRequest {

    private Long id; // Identifier field

    // Basic information
    private String line1;
    private String line2;
    private String postalCode;
    private SettlementType settlementType;
    private Long settlementId;
    private String externalSource;
    private String externalPlaceId;
    private String formattedAddress;
    
    // Relationships
    private Long suburbId;
    private Long villageLocationNodeId;
    /** Denormalized first-class {@code city_id}; must align with suburb/village district when supplied. */
    private Long cityId;
    private Long geoCoordinatesId;
    private BigDecimal latitude;
    private BigDecimal longitude;
}