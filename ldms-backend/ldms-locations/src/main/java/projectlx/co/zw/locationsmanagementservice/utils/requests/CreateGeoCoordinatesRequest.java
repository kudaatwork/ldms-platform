package projectlx.co.zw.locationsmanagementservice.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import java.math.BigDecimal;

@Getter
@Setter
@ToString
public class CreateGeoCoordinatesRequest {

    // Coordinate information
    private BigDecimal latitude;
    private BigDecimal longitude;
}