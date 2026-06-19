package projectlx.co.zw.shared_library.utils.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum OwnerType {
    USER("USER"),
    ORGANIZATION("ORGANIZATION"),
    FLEET_ASSET("FLEET_ASSET"),
    FLEET_DRIVER("FLEET_DRIVER"),
    /** Staging owner for unauthenticated driver signup document uploads (ownerId = stagingSessionId). */
    FLEET_DRIVER_SIGNUP("FLEET_DRIVER_SIGNUP");

    private final String ownerType;
}
