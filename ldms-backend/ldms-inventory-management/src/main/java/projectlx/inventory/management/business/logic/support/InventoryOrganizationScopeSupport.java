package projectlx.inventory.management.business.logic.support;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import projectlx.co.zw.shared_library.model.OrganizationClassification;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.responses.OrganizationResponse;
import projectlx.co.zw.shared_library.utils.responses.UserResponse;
import projectlx.inventory.management.clients.OrganizationServiceClient;
import projectlx.inventory.management.clients.UserManagementServiceClient;
import projectlx.inventory.management.model.WarehouseLocation;
import projectlx.inventory.management.model.WarehouseLocationType;
import projectlx.inventory.management.repository.InventoryItemRepository;
import projectlx.inventory.management.repository.WarehouseLocationRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Resolves caller organisation context and warehouse visibility for inventory reads.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryOrganizationScopeSupport {

    private final UserManagementServiceClient userManagementServiceClient;
    private final OrganizationServiceClient organizationServiceClient;
    private final WarehouseLocationRepository warehouseLocationRepository;
    private final WarehouseAccessSupport warehouseAccessSupport;
    private final InventoryItemRepository inventoryItemRepository;

    public boolean isSystemUser(String username) {
        return username != null && "SYSTEM".equalsIgnoreCase(username.trim());
    }

    public Long resolveOrganizationId(String username, Locale locale) {
        if (username == null || username.isBlank()) {
            return null;
        }
        String principal = username.trim();
        try {
            UserResponse userResponse = userManagementServiceClient.findSessionProfileByUsername(principal);
            if (userResponse != null && userResponse.isSuccess() && userResponse.getUserDto() != null
                    && userResponse.getUserDto().getOrganizationId() != null
                    && userResponse.getUserDto().getOrganizationId() > 0) {
                return userResponse.getUserDto().getOrganizationId();
            }
        } catch (Exception ex) {
            log.warn("Failed to resolve organization via session profile for user {}: {}", principal, ex.getMessage());
        }
        try {
            UserResponse userResponse = userManagementServiceClient.findByPhoneNumberOrEmail(principal, locale);
            if (userResponse != null && userResponse.isSuccess() && userResponse.getUserDto() != null
                    && userResponse.getUserDto().getOrganizationId() != null
                    && userResponse.getUserDto().getOrganizationId() > 0) {
                return userResponse.getUserDto().getOrganizationId();
            }
        } catch (Exception ex) {
            log.warn("Failed to resolve organization for user {}: {}", principal, ex.getMessage());
        }
        return null;
    }

    public boolean isCustomerOrganization(Long organizationId, Locale locale) {
        return OrganizationClassification.CUSTOMER == resolveClassification(organizationId, locale);
    }

    public boolean isSupplierOrganization(Long organizationId, Locale locale) {
        return OrganizationClassification.SUPPLIER == resolveClassification(organizationId, locale);
    }

    public OrganizationClassification resolveClassification(Long organizationId, Locale locale) {
        if (organizationId == null || organizationId <= 0) {
            return null;
        }
        try {
            OrganizationResponse response = organizationServiceClient.findById(organizationId, locale);
            if (response != null && response.isSuccess() && response.getOrganizationDto() != null) {
                return response.getOrganizationDto().getOrganizationClassification();
            }
        } catch (Exception ex) {
            log.warn("Failed to resolve organisation classification for org {}: {}", organizationId, ex.getMessage());
        }
        return null;
    }

    /** Align warehouse type with owning organisation classification (repairs legacy V10 backfill). */
    public WarehouseLocationType expectedWarehouseTypeForOwner(Long owningOrganizationId, Locale locale) {
        if (isCustomerOrganization(owningOrganizationId, locale)) {
            return WarehouseLocationType.CUSTOMER;
        }
        return WarehouseLocationType.SUPPLIER;
    }

    /** Warehouses the caller may view (owned or explicitly shared). */
    public Set<Long> visibleWarehouseIds(Long organizationId) {
        Set<Long> ids = new HashSet<>();
        if (organizationId == null || organizationId <= 0) {
            return ids;
        }
        Set<Long> sharedIds = warehouseAccessSupport.sharedWarehouseIdsForOrganization(organizationId);
        List<WarehouseLocation> candidates = warehouseLocationRepository.findAll();
        for (WarehouseLocation warehouse : candidates) {
            if (warehouse.getEntityStatus() == EntityStatus.DELETED || warehouse.isVirtualWarehouse()) {
                continue;
            }
            if (organizationId.equals(warehouse.getSupplierId()) || sharedIds.contains(warehouse.getId())) {
                ids.add(warehouse.getId());
            }
        }
        return ids;
    }

    /** Product ids with inventory rows at the caller's visible warehouses. */
    public List<Long> distinctProductIdsAtWarehouses(Set<Long> warehouseIds) {
        if (warehouseIds == null || warehouseIds.isEmpty()) {
            return List.of();
        }
        return inventoryItemRepository.findDistinctProductIdsByWarehouseLocationIdIn(warehouseIds, EntityStatus.DELETED);
    }

    /** True when the record belongs to the signed-in organisation (system users bypass). */
    public boolean isOwnedByCaller(Long owningOrganizationId, String username, Locale locale) {
        if (isSystemUser(username)) {
            return true;
        }
        Long callerOrgId = resolveOrganizationId(username, locale);
        return callerOrgId != null && owningOrganizationId != null && callerOrgId.equals(owningOrganizationId);
    }

    /** Caller organisation id for tenant-scoped reads; null for system users (no mandatory filter). */
    public Long callerOrganizationId(String username, Locale locale) {
        if (isSystemUser(username)) {
            return null;
        }
        return resolveOrganizationId(username, locale);
    }
}
