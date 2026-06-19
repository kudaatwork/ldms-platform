package projectlx.co.zw.organizationmanagement.utils.requests;

import lombok.Getter;
import lombok.Setter;
import projectlx.co.zw.shared_library.utils.enums.TradingPartnerRole;

/**
 * Request for creating a new trading partner entry.
 *
 * <p>Set {@code linkedOrganizationId} when the counterparty is already registered on LDMS.
 * When left null, the partner is created as a CRM-only record ({@code recordOnly = true}).</p>
 */
@Getter
@Setter
public class CreateTradingPartnerRequest {

    private TradingPartnerRole partnerRole;
    private String name;
    private String email;
    private String phone;
    private Long locationId;
    private String notes;

    /** Optional: platform organisation id if the partner is also on LDMS. */
    private Long linkedOrganizationId;
}
