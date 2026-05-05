package projectlx.co.zw.locationsmanagementservice.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.locationsmanagementservice.utils.enums.LocationType;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@ToString
public class CreateLocationNodeRequest {
    private String name;
    private String code;
    private LocationType locationType;
    private Long parentId;
    private Long districtId;
    private Long suburbId;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String timezone;
    private String postalCode;
    private List<String> aliases;
}
