package projectlx.co.zw.organizationmanagement.business.logic.support;

import projectlx.co.zw.organizationmanagement.model.Organization;
import projectlx.co.zw.organizationmanagement.model.OrganizationClassification;

/**
 * Determines which trading hats an organisation may wear.
 * Duplex orgs keep their primary classification but may use both customer and supplier capabilities.
 */
public final class OrganizationTradingCapabilitySupport {

    private OrganizationTradingCapabilitySupport() {
    }

    public static boolean canActAsSupplier(Organization organization) {
        if (organization == null) {
            return false;
        }
        return organization.getOrganizationClassification() == OrganizationClassification.SUPPLIER
                || organization.isDuplexMode();
    }

    public static boolean canActAsCustomer(Organization organization) {
        if (organization == null) {
            return false;
        }
        return organization.getOrganizationClassification() == OrganizationClassification.CUSTOMER
                || organization.isDuplexMode();
    }

    /** Supplier or customer workspaces (including duplex) may contract transport companies. */
    public static boolean canContractTransporters(Organization organization) {
        return canActAsSupplier(organization) || canActAsCustomer(organization);
    }

    /**
     * Whether this org may be linked as a customer of another supplier.
     */
    public static boolean canBeLinkedAsCustomer(Organization target, boolean enablingDuplex) {
        if (target == null) {
            return false;
        }
        if (target.getOrganizationClassification() == OrganizationClassification.CUSTOMER) {
            return true;
        }
        if (target.getOrganizationClassification() == OrganizationClassification.SUPPLIER) {
            return enablingDuplex || target.isDuplexMode();
        }
        return target.isDuplexMode();
    }
}
