package projectlx.co.zw.organizationmanagement.utils.requests;

import lombok.Getter;
import lombok.Setter;
import projectlx.co.zw.shared_library.utils.enums.InventoryDataSource;

/**
 * Request payload for updating an organisation's operational mode settings.
 *
 * <p>All fields are optional; only non-null values are applied. Mutual-exclusivity rules
 * (inventory management vs cross-docking) are enforced by {@code OrganizationOperationalModeSupport}.</p>
 */
@Getter
@Setter
public class UpdateOrganizationOperationalSettingsRequest {

    /** When true, org runs solo logistics; counterparty is a CRM/trading-partner record only. */
    private Boolean standaloneMode;

    /** When true, full stock management is enabled via the LDMS Inventory module. */
    private Boolean inventoryManagementEnabled;

    /** When true, org operates cross-dock logistics (mutually exclusive with full inventory management). */
    private Boolean crossDockingEnabled;

    /** How inventory data is sourced. Ignored when {@code inventoryManagementEnabled} is {@code true}. */
    private InventoryDataSource inventoryDataSource;

    /**
     * When standalone mode is off: RECORD_ONLY (CRM records, no counterparty login) or
     * PLATFORM_ORG (register or link platform organisations).
     */
    private projectlx.co.zw.shared_library.utils.enums.CounterpartyEngagementMode counterpartyEngagementMode;
}
