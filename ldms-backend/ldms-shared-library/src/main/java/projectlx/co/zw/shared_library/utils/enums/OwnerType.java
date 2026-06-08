package projectlx.co.zw.shared_library.utils.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum OwnerType {
    USER("USER"),
    ORGANIZATION("ORGANIZATION"),
    FLEET_ASSET("FLEET_ASSET"),
    FLEET_DRIVER("FLEET_DRIVER");

    private final String ownerType;
}
