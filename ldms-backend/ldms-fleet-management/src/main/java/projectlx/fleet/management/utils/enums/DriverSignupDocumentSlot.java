package projectlx.fleet.management.utils.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import projectlx.co.zw.shared_library.utils.enums.FileType;

@RequiredArgsConstructor
@Getter
public enum DriverSignupDocumentSlot {
    NATIONAL_ID_FRONT(FileType.NATIONAL_ID),
    NATIONAL_ID_BACK(FileType.NATIONAL_ID),
    LICENSE_FRONT(FileType.DRIVER_LICENCE),
    LICENSE_BACK(FileType.DRIVER_LICENCE);

    private final FileType fileType;
}
