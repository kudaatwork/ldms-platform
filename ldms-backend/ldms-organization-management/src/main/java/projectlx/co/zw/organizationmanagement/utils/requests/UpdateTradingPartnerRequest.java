package projectlx.co.zw.organizationmanagement.utils.requests;

import lombok.Getter;
import lombok.Setter;
import projectlx.co.zw.shared_library.utils.enums.TradingPartnerRole;

/**
 * Request for updating an existing trading partner entry.
 * All fields are optional; only non-null values are applied.
 */
@Getter
@Setter
public class UpdateTradingPartnerRequest {

    private TradingPartnerRole partnerRole;
    private String name;
    private String email;
    private String phone;
    private Long locationId;
    private String notes;
    private Long linkedOrganizationId;
}
