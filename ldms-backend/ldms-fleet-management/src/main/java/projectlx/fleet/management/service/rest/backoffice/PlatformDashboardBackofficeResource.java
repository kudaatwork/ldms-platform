package projectlx.fleet.management.service.rest.backoffice;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;
import projectlx.fleet.management.service.processor.api.FleetDashboardServiceProcessor;
import projectlx.fleet.management.utils.responses.PlatformFleetDashboardResponse;

import java.util.Locale;

@CrossOrigin
@RestController
@RequestMapping("/ldms-fleet-management/v1/backoffice/platform-dashboard")
@Tag(name = "Platform dashboard (backoffice)", description = "Cross-tenant fleet metrics for LX administrators")
@RequiredArgsConstructor
public class PlatformDashboardBackofficeResource {

    private final FleetDashboardServiceProcessor fleetDashboardServiceProcessor;

    @Auditable(action = "BACKOFFICE_PLATFORM_FLEET_DASHBOARD")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/fleet")
    @Operation(summary = "Cross-tenant fleet dashboard snapshot")
    public ResponseEntity<PlatformFleetDashboardResponse> fleetDashboard(
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        PlatformFleetDashboardResponse response = fleetDashboardServiceProcessor.getPlatformDashboard(locale);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }
}
