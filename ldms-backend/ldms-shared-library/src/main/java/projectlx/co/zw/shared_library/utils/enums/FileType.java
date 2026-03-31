package projectlx.co.zw.shared_library.utils.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum FileType {
    NATIONAL_ID("NATIONAL_ID"),
    PASSPORT("PASSPORT"),
    COMPANY_REGISTRATION_CERTIFICATE("COMPANY_REGISTRATION_CERTIFICATE"),
    TAX_CLEARANCE_CERTIFICATE("TAX_CLEARANCE_CERTIFICATE"),
    BUSINESS_LICENSE("BUSINESS_LICENSE"),
    PROOF_OF_ADDRESS("PROOF_OF_ADDRESS"),
    INDUSTRY_SPECIFIC_LICENSE("INDUSTRY_SPECIFIC_LICENSE"),
    ORGANIZATION_LOGO("ORGANIZATION_LOGO"),
    PRODUCT("PRODUCT"),
    OTHER("OTHER");

    private final String displayName;
}
