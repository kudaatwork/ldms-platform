package projectlx.fleet.management.clients;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import projectlx.fleet.management.utils.requests.FleetRegisteredNotificationRequest;
import projectlx.fleet.management.utils.requests.ValidateFleetOwnershipRequest;
import projectlx.co.zw.shared_library.utils.responses.OrganizationResponse;

public interface OrganizationManagementServiceClient {

    @PostMapping("/ldms-organization-management/v1/system/organization/fleet-registered/notify")
    OrganizationResponse notifyFleetRegistered(@RequestBody FleetRegisteredNotificationRequest request);

    @PostMapping("/ldms-organization-management/v1/system/organization/fleet-ownership/validate")
    OrganizationResponse validateFleetOwnership(@RequestBody ValidateFleetOwnershipRequest request);
}
