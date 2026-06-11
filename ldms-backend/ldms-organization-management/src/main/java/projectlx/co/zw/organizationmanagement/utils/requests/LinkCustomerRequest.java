package projectlx.co.zw.organizationmanagement.utils.requests;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LinkCustomerRequest {

    private Long customerOrganizationId;

    /** When linking a supplier organisation as a customer, set true to enable duplex mode. */
    private Boolean enableDuplexMode;
}
