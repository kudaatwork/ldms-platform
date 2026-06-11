package projectlx.fleet.management.business.logic.support;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import projectlx.fleet.management.clients.OrganizationManagementServiceClient;
import projectlx.fleet.management.model.FleetAsset;
import projectlx.fleet.management.utils.enums.FleetOwnershipType;
import projectlx.fleet.management.utils.requests.FleetRegisteredNotificationRequest;

@Component
@RequiredArgsConstructor
@Slf4j
public class FleetAssetRegistrationNotificationSupport {

    private final OrganizationManagementServiceClient organizationManagementServiceClient;

    public void scheduleAfterCommit(FleetAsset asset) {
        if (asset == null || asset.getOrganizationId() == null) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            sendRegistrationNotification(asset);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                sendRegistrationNotification(asset);
            }
        });
    }

    private void sendRegistrationNotification(FleetAsset asset) {
        try {
            FleetRegisteredNotificationRequest request = new FleetRegisteredNotificationRequest();
            request.setRegisteringOrganizationId(asset.getOrganizationId());
            request.setContractedTransporterOrganizationId(asset.getContractedTransporterOrganizationId());
            FleetOwnershipType ownershipType = asset.getOwnershipType() != null
                    ? asset.getOwnershipType()
                    : FleetOwnershipType.OWNED;
            request.setOwnershipType(ownershipType.name());
            request.setRegistration(asset.getRegistration());
            request.setMakeModel(asset.getMakeModel());
            request.setAssetType(asset.getAssetType() != null ? asset.getAssetType().name() : null);
            request.setPerformedBy(asset.getCreatedBy());
            organizationManagementServiceClient.notifyFleetRegistered(request);
        } catch (Exception ex) {
            log.warn(
                    "Fleet registration notification failed for asset orgId={} registration={}: {}",
                    asset.getOrganizationId(),
                    asset.getRegistration(),
                    ex.getMessage());
        }
    }
}
