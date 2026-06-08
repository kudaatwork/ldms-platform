package projectlx.co.zw.organizationmanagement.clients.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LocationAddressDto {

    private Long id;
    private String line1;
    private String line2;
    private String postalCode;
    private Long suburbId;
    private Long cityId;
    private String cityName;
    private Long districtId;
    private Long provinceId;
    private Long countryId;
}
