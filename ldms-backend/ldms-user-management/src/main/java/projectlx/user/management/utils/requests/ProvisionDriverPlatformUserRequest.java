package projectlx.user.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Request to provision a fleet driver user account on the platform.
 * Called by ldms-fleet-management after a driver signup request is approved
 * or when a new driver is created with {@code provisionPlatformAccess=true}.
 */
@Getter
@Setter
@ToString
public class ProvisionDriverPlatformUserRequest {

    /** Required — the organisation the driver will be linked to. */
    private Long organizationId;

    /** Required — driver first name. */
    private String firstName;

    /** Required — driver last name. */
    private String lastName;

    /** Required — driver email address (used as unique login handle). */
    private String email;

    /** Optional — driver phone number (E.164 preferred). */
    private String phoneNumber;

    /** Optional — driver licence number stored on the user profile. */
    private String licenseNumber;

    /**
     * Optional — when provided, reuse this platform user id instead of
     * finding/creating by email.  The user must already exist and not be DELETED.
     */
    private Long existingUserId;
}
