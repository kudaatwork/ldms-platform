package projectlx.co.zw.locationsmanagementservice.utils.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.locationsmanagementservice.utils.enums.SettlementType;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AddressDto {

    private Long id;
    
    private String line1;
    private String line2;
    private String postalCode;
    private SettlementType settlementType;
    private Long settlementId;
    private String externalSource;
    private String externalPlaceId;
    private String formattedAddress;
    
    private Long suburbId;
    private String suburbName;
    
    private Long districtId;
    private String districtName;
    
    private Long provinceId;
    private String provinceName;
    
    private Long countryId;
    private String countryName;

    private Long cityId;
    private String cityName;
    private Long villageId;
    private String villageName;
    
    private Long geoCoordinatesId;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    private EntityStatus entityStatus;
}