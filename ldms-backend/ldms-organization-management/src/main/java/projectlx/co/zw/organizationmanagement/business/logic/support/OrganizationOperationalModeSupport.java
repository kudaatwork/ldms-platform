package projectlx.co.zw.organizationmanagement.business.logic.support;

import projectlx.co.zw.organizationmanagement.model.Organization;
import projectlx.co.zw.organizationmanagement.utils.enums.I18Code;
import projectlx.co.zw.shared_library.utils.enums.CounterpartyEngagementMode;
import projectlx.co.zw.shared_library.utils.enums.InventoryDataSource;

import java.util.Optional;

/**
 * Validates and applies operational mode settings for an organisation.
 *
 * <h3>Allowed transitions</h3>
 * <ul>
 *   <li>cross-dock → full inventory: enable {@code inventoryManagementEnabled}, disable
 *       {@code crossDockingEnabled}, set {@code inventoryDataSource = INTERNAL}. <strong>Always allowed.</strong></li>
 *   <li>full inventory → cross-dock: <strong>BLOCKED</strong>. A supplier that already has full inventory
 *       management cannot be downgraded to cross-docking without a manual data-cleanup process.</li>
 *   <li>{@code INTERNAL} (with full inventory) → {@code EXTERNAL_API}: <strong>BLOCKED</strong>. Changing the
 *       data source away from INTERNAL while inventory management is active would silently orphan stock records.</li>
 *   <li>{@code EXTERNAL_API} or {@code MANUAL_ACK} → {@code INTERNAL} (when enabling inventory management):
 *       <strong>ALLOWED</strong>. Consolidating to the built-in module is a safe upgrade path.</li>
 * </ul>
 */
public final class OrganizationOperationalModeSupport {

    private OrganizationOperationalModeSupport() {
    }

    /**
     * Validates whether the requested operational mode transition is permitted.
     *
     * @param current                   the current persisted organisation state
     * @param standaloneMode            requested standalone mode flag
     * @param inventoryManagementEnabled requested inventory management flag
     * @param crossDockingEnabled       requested cross-docking flag
     * @param inventoryDataSource       requested inventory data source
     * @return an empty Optional when the transition is valid, or an Optional containing the i18n error
     *         code ({@link I18Code#getCode()}) when the transition is blocked
     */
    public static Optional<String> validateTransition(
            Organization current,
            Boolean standaloneMode,
            Boolean inventoryManagementEnabled,
            Boolean crossDockingEnabled,
            InventoryDataSource inventoryDataSource) {

        boolean currentlyHasInventoryMgmt = current.isInventoryManagementEnabled();
        boolean effectiveInventoryMgmt = inventoryManagementEnabled != null
                ? Boolean.TRUE.equals(inventoryManagementEnabled)
                : currentlyHasInventoryMgmt;
        boolean requestingCrossDock = Boolean.TRUE.equals(crossDockingEnabled);
        InventoryDataSource effectiveDataSource = inventoryDataSource != null
                ? inventoryDataSource
                : current.getInventoryDataSource();

        // BLOCKED: downgrading from full inventory to cross-docking
        if (currentlyHasInventoryMgmt && requestingCrossDock && !effectiveInventoryMgmt) {
            return Optional.of(I18Code.ORG_OP_CANNOT_DOWNGRADE_TO_CROSS_DOCK.getCode());
        }

        // BLOCKED: switching data source from INTERNAL to EXTERNAL_API while full inventory management remains active
        if (effectiveInventoryMgmt
                && current.getInventoryDataSource() == InventoryDataSource.INTERNAL
                && effectiveDataSource == InventoryDataSource.EXTERNAL_API) {
            return Optional.of(I18Code.ORG_OP_CANNOT_CHANGE_DATA_SOURCE_TO_EXTERNAL.getCode());
        }

        return Optional.empty();
    }

    /**
     * Applies validated operational settings to the organisation entity, enforcing mutual-exclusivity rules:
     * <ul>
 *   <li>If {@code inventoryManagementEnabled} is {@code true}: {@code crossDockingEnabled} is forced
 *       to {@code false}. {@code inventoryDataSource} may be {@code INTERNAL} (capture in LDMS) or
 *       {@code EXTERNAL_API} (ERP/WMS sync). {@code MANUAL_ACK} is not used with full inventory.</li>
     *   <li>If {@code crossDockingEnabled} is {@code true}: {@code inventoryManagementEnabled} is forced
     *       to {@code false}.</li>
     * </ul>
     *
     * @param org                        the organisation entity to mutate
     * @param standaloneMode             new standalone mode value
     * @param inventoryManagementEnabled new inventory management flag
     * @param crossDockingEnabled        new cross-docking flag
     * @param inventoryDataSource        new inventory data source
     */
    public static void applySettings(
            Organization org,
            Boolean standaloneMode,
            Boolean inventoryManagementEnabled,
            Boolean crossDockingEnabled,
            InventoryDataSource inventoryDataSource,
            CounterpartyEngagementMode counterpartyEngagementMode) {

        if (standaloneMode != null) {
            org.setStandaloneMode(standaloneMode);
        }

        boolean enableInventoryMgmt = Boolean.TRUE.equals(inventoryManagementEnabled);
        boolean enableCrossDock = Boolean.TRUE.equals(crossDockingEnabled);

        if (enableInventoryMgmt) {
            org.setInventoryManagementEnabled(true);
            org.setCrossDockingEnabled(false);
            if (inventoryDataSource != null) {
                org.setInventoryDataSource(inventoryDataSource);
            } else if (org.getInventoryDataSource() == null
                    || org.getInventoryDataSource() == InventoryDataSource.MANUAL_ACK) {
                org.setInventoryDataSource(InventoryDataSource.INTERNAL);
            }
        } else if (enableCrossDock) {
            org.setCrossDockingEnabled(true);
            org.setInventoryManagementEnabled(false);
            if (inventoryDataSource != null) {
                org.setInventoryDataSource(inventoryDataSource);
            }
        } else {
            if (inventoryManagementEnabled != null) {
                org.setInventoryManagementEnabled(inventoryManagementEnabled);
            }
            if (crossDockingEnabled != null) {
                org.setCrossDockingEnabled(crossDockingEnabled);
            }
            if (inventoryDataSource != null) {
                org.setInventoryDataSource(inventoryDataSource);
            }
        }

        if (org.isStandaloneMode()) {
            org.setCounterpartyEngagementMode(CounterpartyEngagementMode.RECORD_ONLY);
        } else if (counterpartyEngagementMode != null) {
            org.setCounterpartyEngagementMode(counterpartyEngagementMode);
        } else if (org.getCounterpartyEngagementMode() == null) {
            org.setCounterpartyEngagementMode(CounterpartyEngagementMode.PLATFORM_ORG);
        }
    }

    /** @deprecated use {@link #applySettings(Organization, Boolean, Boolean, Boolean, InventoryDataSource, CounterpartyEngagementMode)} */
    public static void applySettings(
            Organization org,
            Boolean standaloneMode,
            Boolean inventoryManagementEnabled,
            Boolean crossDockingEnabled,
            InventoryDataSource inventoryDataSource) {
        applySettings(org, standaloneMode, inventoryManagementEnabled, crossDockingEnabled, inventoryDataSource, null);
    }
}
