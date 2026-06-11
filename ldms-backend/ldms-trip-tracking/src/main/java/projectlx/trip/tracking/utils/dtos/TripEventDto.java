package projectlx.trip.tracking.utils.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.trip.tracking.utils.enums.TripEventType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TripEventDto {

    private Long id;
    private Long tripId;
    private TripEventType eventType;
    private LocalDateTime eventTime;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String notes;
    private Long recordedByUserId;
    private LocalDateTime createdAt;
}
