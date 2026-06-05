package projectlx.co.zw.organizationmanagement.utils.requests;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LinkTransporterRequest {

    private Long transporterOrganizationId;

    /** ISO date {@code yyyy-MM-dd}. */
    private String contractStartDate;
    /** ISO date {@code yyyy-MM-dd}; optional open-ended contract when omitted. */
    private String contractEndDate;
}
