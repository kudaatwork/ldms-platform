package projectlx.fleet.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Enables platform login for a legacy fleet driver (no linked {@code userId}) or
 * re-issues temporary credentials when {@code reissueCredentials=true}.
 */
@Getter
@Setter
@ToString
public class ProvisionFleetDriverPlatformAccessRequest {

    /** Required — email address where temporary credentials are sent. */
    private String email;

    /**
     * When {@code true}, issues new temporary credentials for a driver that already has
     * a linked platform user. Ignored when the driver has no {@code userId}.
     */
    private Boolean reissueCredentials;
}
