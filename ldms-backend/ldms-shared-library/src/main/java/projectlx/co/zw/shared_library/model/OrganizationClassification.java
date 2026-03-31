package projectlx.co.zw.shared_library.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum OrganizationClassification {

    SUPPLIER("SUPPLIER"),
    CUSTOMER("CUSTOMER"),
    SERVICE_STATION("SERVICE_STATION"),
    ROADSIDE_SUPPORT_SERVICE("ROADSIDE_SUPPORT_SERVICE"),
    TRANSPORT_COMPANY("TRANSPORT_COMPANY"),
    CLEARING_AGENT("CLEARING_AGENT"),
    GOVERNMENT_AGENCY("GOVERNMENT_AGENCY");

    private final String organizationClassification;
}
