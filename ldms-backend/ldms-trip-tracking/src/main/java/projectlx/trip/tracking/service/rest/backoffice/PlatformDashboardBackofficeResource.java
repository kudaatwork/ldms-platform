package projectlx.trip.tracking.service.rest.backoffice;

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
import projectlx.trip.tracking.service.processor.api.PlatformDashboardServiceProcessor;
import projectlx.trip.tracking.utils.responses.PlatformTripDashboardResponse;

import java.util.Locale;

@CrossOrigin
@RestController
@RequestMapping("/ldms-trip-tracking/v1/backoffice/platform-dashboard")
@Tag(name = "Platform dashboard (backoffice)", description = "Cross-tenant trip metrics for LX administrators")
@RequiredArgsConstructor
public class PlatformDashboardBackofficeResource {

    private final PlatformDashboardServiceProcessor platformDashboardServiceProcessor;

    @Auditable(action = "BACKOFFICE_PLATFORM_TRIP_DASHBOARD")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/trips")
    @Operation(summary = "Cross-tenant trip dashboard snapshot")
    public ResponseEntity<PlatformTripDashboardResponse> tripDashboard(
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        PlatformTripDashboardResponse response = platformDashboardServiceProcessor.getTripDashboard(locale);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }
}
