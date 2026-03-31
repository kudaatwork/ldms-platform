package projectlx.co.zw.locationsmanagementservice.utils.requests;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
@RequiredArgsConstructor
public class EditGeoCoordinatesRequest {

    private Long id; // Identifier field

    // Coordinate information
    private BigDecimal latitude;
    private BigDecimal longitude;
}