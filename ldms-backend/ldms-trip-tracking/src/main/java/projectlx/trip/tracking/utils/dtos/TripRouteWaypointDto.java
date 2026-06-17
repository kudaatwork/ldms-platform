package projectlx.trip.tracking.utils.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TripRouteWaypointDto {

    private String label;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String type;
    private BigDecimal speedKmh;
    private LocalDateTime recordedAt;
}
