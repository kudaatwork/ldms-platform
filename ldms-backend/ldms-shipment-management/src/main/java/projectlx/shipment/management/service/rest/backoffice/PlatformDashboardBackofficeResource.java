package projectlx.shipment.management.service.rest.backoffice;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;
import projectlx.shipment.management.service.processor.api.PlatformDashboardServiceProcessor;
import projectlx.shipment.management.utils.responses.PlatformShipmentDashboardResponse;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@CrossOrigin
@RestController
@RequestMapping("/ldms-shipment-management/v1/backoffice/platform-dashboard")
@Tag(name = "Platform dashboard (backoffice)", description = "Cross-tenant shipment metrics for LX administrators")
@RequiredArgsConstructor
public class PlatformDashboardBackofficeResource {

    private final PlatformDashboardServiceProcessor platformDashboardServiceProcessor;

    @Auditable(action = "BACKOFFICE_PLATFORM_SHIPMENT_DASHBOARD")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/shipments")
    @Operation(summary = "Cross-tenant shipment dashboard snapshot")
    public ResponseEntity<PlatformShipmentDashboardResponse> shipmentDashboard(
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        PlatformShipmentDashboardResponse response = platformDashboardServiceProcessor.getShipmentDashboard(locale);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "BACKOFFICE_PLATFORM_SHIPMENT_SEARCH")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/shipments/search")
    @Operation(summary = "Cross-tenant shipment search for LX Admin")
    public ResponseEntity<PlatformShipmentDashboardResponse> searchShipments(
            @RequestParam String term,
            @RequestParam(required = false) String purchaseOrderIds,
            @RequestParam(defaultValue = "25") int limit,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        List<Long> poIds = parseIdList(purchaseOrderIds);
        PlatformShipmentDashboardResponse response = platformDashboardServiceProcessor.searchShipments(
                term, poIds, limit, locale);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    private static List<Long> parseIdList(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .toList();
    }
}
