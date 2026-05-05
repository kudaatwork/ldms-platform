package projectlx.co.zw.locationsmanagementservice.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.locationsmanagementservice.utils.enums.SettlementType;

import java.math.BigDecimal;

@Getter
@Setter
@ToString
public class CreateAddressRequest {

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
    /** Denormalized first-class {@code city_id} on address; must match settlement's district when provided. */
    private Long cityId;
    private Long geoCoordinatesId;
    private BigDecimal latitude;
    private BigDecimal longitude;
}