package projectlx.co.zw.organizationmanagement.utils.requests;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LinkExistingOrganizationAsCustomerRequest {

    /** Existing organisation id to link as a customer of the signed-in supplier. */
    private Long existingOrganizationId;

    /**
     * Enable duplex on the linked org when it primarily operates as a supplier.
     * Ignored when the target is already a customer.
     */
    private Boolean enableDuplexMode;
}
