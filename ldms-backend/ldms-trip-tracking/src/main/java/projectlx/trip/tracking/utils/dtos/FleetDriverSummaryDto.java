package projectlx.trip.tracking.utils.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Lightweight fleet driver projection used when resolving driver contact details
 * for logistics lifecycle notifications.
 */
@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class FleetDriverSummaryDto {
    private Long id;
    private Long organizationId;
    private Long userId;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String licenseNumber;
}
