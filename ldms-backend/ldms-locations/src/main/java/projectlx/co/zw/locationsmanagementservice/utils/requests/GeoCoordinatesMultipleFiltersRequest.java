package projectlx.co.zw.locationsmanagementservice.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.shared_library.utils.requests.MultipleFiltersRequest;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import java.math.BigDecimal;

@Getter
@Setter
@ToString
public class GeoCoordinatesMultipleFiltersRequest extends MultipleFiltersRequest {
    private BigDecimal latitude;
    private BigDecimal longitude;
    private EntityStatus entityStatus;
}