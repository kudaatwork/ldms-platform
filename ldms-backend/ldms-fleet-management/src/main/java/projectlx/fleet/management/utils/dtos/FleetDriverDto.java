package projectlx.fleet.management.utils.dtos;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
public class FleetDriverDto {
    private Long id;
    private Long organizationId;
    private Long userId;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String licenseNumber;
    private String licenseClass;
    private String entityStatus;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime modifiedAt;
    private String modifiedBy;
}
