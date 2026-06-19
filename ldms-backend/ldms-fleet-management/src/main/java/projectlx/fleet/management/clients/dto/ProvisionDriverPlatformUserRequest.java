package projectlx.fleet.management.clients.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Feign DTO mirroring {@code projectlx.user.management.utils.requests.ProvisionDriverPlatformUserRequest}.
 * Used when calling the user-management service to provision driver platform access.
 */
@Getter
@Setter
@ToString
public class ProvisionDriverPlatformUserRequest {

    private Long organizationId;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String licenseNumber;
    private Long existingUserId;
}
