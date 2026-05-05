package projectlx.co.zw.locationsmanagementservice.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@Setter
@ToString
public class CreateCityRequest {

    private String name;
    private String code;
    private Long districtId;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String timezone;
    private String postalCode;
}
