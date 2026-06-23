package projectlx.fleet.management.clients;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import projectlx.fleet.management.utils.requests.FleetRegisteredNotificationRequest;
import projectlx.fleet.management.utils.requests.ValidateFleetOwnershipRequest;
import projectlx.co.zw.shared_library.utils.constants.Constants;
import projectlx.co.zw.shared_library.utils.responses.OrganizationResponse;

import java.util.Locale;

public interface OrganizationManagementServiceClient {

    @GetMapping("/ldms-organization-management/v1/system/organization/{id}")
    OrganizationResponse findById(
            @PathVariable("id") Long id,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale);

    @PostMapping("/ldms-organization-management/v1/system/organization/fleet-registered/notify")
    OrganizationResponse notifyFleetRegistered(@RequestBody FleetRegisteredNotificationRequest request);

    @PostMapping("/ldms-organization-management/v1/system/organization/fleet-ownership/validate")
    OrganizationResponse validateFleetOwnership(@RequestBody ValidateFleetOwnershipRequest request);
}
