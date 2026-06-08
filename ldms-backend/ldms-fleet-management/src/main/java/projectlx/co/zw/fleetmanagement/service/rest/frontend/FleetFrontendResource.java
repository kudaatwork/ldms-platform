package projectlx.co.zw.fleetmanagement.service.rest.frontend;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import projectlx.co.zw.fleetmanagement.service.processor.api.FleetAssetServiceProcessor;
import projectlx.co.zw.fleetmanagement.service.processor.api.FleetComplianceServiceProcessor;
import projectlx.co.zw.fleetmanagement.service.processor.api.FleetDriverServiceProcessor;
import projectlx.co.zw.fleetmanagement.utils.requests.CreateFleetAssetRequest;
import projectlx.co.zw.fleetmanagement.utils.requests.CreateFleetComplianceRecordRequest;
import projectlx.co.zw.fleetmanagement.utils.requests.CreateFleetDriverRequest;
import projectlx.co.zw.fleetmanagement.utils.requests.EditFleetAssetRequest;
import projectlx.co.zw.fleetmanagement.utils.requests.EditFleetComplianceRecordRequest;
import projectlx.co.zw.fleetmanagement.utils.requests.EditFleetDriverRequest;
import projectlx.co.zw.fleetmanagement.utils.responses.FleetAssetResponse;
import projectlx.co.zw.fleetmanagement.utils.responses.FleetComplianceRecordResponse;
import projectlx.co.zw.fleetmanagement.utils.responses.FleetDriverResponse;
import projectlx.co.zw.fleetmanagement.utils.security.FleetRoles;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;

import java.util.Locale;

@CrossOrigin
@RestController
@RequestMapping("/ldms-fleet-management/v1/frontend/fleet")
@Tag(name = "Fleet Frontend Resource", description = "Fleet assets, drivers, and compliance management")
@RequiredArgsConstructor
public class FleetFrontendResource {

    private static final Logger logger = LoggerFactory.getLogger(FleetFrontendResource.class);

    private final FleetAssetServiceProcessor fleetAssetServiceProcessor;
    private final FleetDriverServiceProcessor fleetDriverServiceProcessor;
    private final FleetComplianceServiceProcessor fleetComplianceServiceProcessor;

    @Auditable(action = "LIST_FLEET_ASSETS")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/assets")
    @Operation(summary = "List fleet assets", description = "Lists fleet assets for the signed-in user's organisation.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fleet assets retrieved"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public ResponseEntity<FleetAssetResponse> listAssets(
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        FleetAssetResponse response = fleetAssetServiceProcessor.list(locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "CREATE_FLEET_ASSET")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/assets")
    @Operation(summary = "Create fleet asset", description = "Creates a fleet asset for the signed-in user's organisation.")
    public ResponseEntity<FleetAssetResponse> createAsset(
            @RequestBody final CreateFleetAssetRequest request,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        FleetAssetResponse response = fleetAssetServiceProcessor.create(request, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "UPDATE_FLEET_ASSET")
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/assets/{id}")
    @Operation(summary = "Update fleet asset", description = "Updates a fleet asset by id.")
    public ResponseEntity<FleetAssetResponse> updateAsset(
            @PathVariable final Long id,
            @RequestBody final EditFleetAssetRequest request,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        FleetAssetResponse response = fleetAssetServiceProcessor.update(id, request, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "DELETE_FLEET_ASSET")
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/assets/{id}")
    @Operation(summary = "Delete fleet asset", description = "Soft-deletes a fleet asset by id.")
    public ResponseEntity<FleetAssetResponse> deleteAsset(
            @PathVariable final Long id,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        FleetAssetResponse response = fleetAssetServiceProcessor.delete(id, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "LIST_FLEET_DRIVERS")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/drivers")
    @Operation(summary = "List fleet drivers", description = "Lists fleet drivers for the signed-in user's organisation.")
    public ResponseEntity<FleetDriverResponse> listDrivers(
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        FleetDriverResponse response = fleetDriverServiceProcessor.list(locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "CREATE_FLEET_DRIVER")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/drivers")
    @Operation(summary = "Create fleet driver", description = "Creates a fleet driver for the signed-in user's organisation.")
    public ResponseEntity<FleetDriverResponse> createDriver(
            @RequestBody final CreateFleetDriverRequest request,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        FleetDriverResponse response = fleetDriverServiceProcessor.create(request, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "UPDATE_FLEET_DRIVER")
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/drivers/{id}")
    @Operation(summary = "Update fleet driver", description = "Updates a fleet driver by id.")
    public ResponseEntity<FleetDriverResponse> updateDriver(
            @PathVariable final Long id,
            @RequestBody final EditFleetDriverRequest request,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        FleetDriverResponse response = fleetDriverServiceProcessor.update(id, request, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "DELETE_FLEET_DRIVER")
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/drivers/{id}")
    @Operation(summary = "Delete fleet driver", description = "Soft-deletes a fleet driver by id.")
    public ResponseEntity<FleetDriverResponse> deleteDriver(
            @PathVariable final Long id,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        FleetDriverResponse response = fleetDriverServiceProcessor.delete(id, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "LIST_FLEET_COMPLIANCE")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/compliance")
    @Operation(summary = "List compliance records", description = "Lists compliance records for the signed-in user's organisation.")
    public ResponseEntity<FleetComplianceRecordResponse> listCompliance(
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        FleetComplianceRecordResponse response = fleetComplianceServiceProcessor.list(locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "CREATE_FLEET_COMPLIANCE")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/compliance")
    @Operation(summary = "Create compliance record", description = "Creates a compliance record linked to an asset or driver.")
    public ResponseEntity<FleetComplianceRecordResponse> createCompliance(
            @RequestBody final CreateFleetComplianceRecordRequest request,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        FleetComplianceRecordResponse response = fleetComplianceServiceProcessor.create(request, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "UPDATE_FLEET_COMPLIANCE")
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/compliance/{id}")
    @Operation(summary = "Update compliance record", description = "Updates expiry, status, or file reference on a compliance record.")
    public ResponseEntity<FleetComplianceRecordResponse> updateCompliance(
            @PathVariable final Long id,
            @RequestBody final EditFleetComplianceRecordRequest request,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        FleetComplianceRecordResponse response = fleetComplianceServiceProcessor.update(id, request, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "LIST_EXPIRING_FLEET_COMPLIANCE")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/compliance/expiring")
    @Operation(summary = "Expiring compliance dashboard", description = "Lists compliance records expiring within the given number of days.")
    public ResponseEntity<FleetComplianceRecordResponse> listExpiringCompliance(
            @RequestParam(defaultValue = "30") final int withinDays,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        FleetComplianceRecordResponse response = fleetComplianceServiceProcessor.listExpiring(withinDays, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }
}
