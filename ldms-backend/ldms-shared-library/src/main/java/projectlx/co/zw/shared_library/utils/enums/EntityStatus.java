package projectlx.co.zw.shared_library.utils.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum EntityStatus {

    ACTIVE("ACTIVE"),      // Active and ready for use
    DELETED("DELETED"),  // Permanently deleted from the system
    INACTIVE("INACTIVE"),
    ARCHIVED("ARCHIVED");  // Inactive, but not yet deleted

    private final String status;
}
