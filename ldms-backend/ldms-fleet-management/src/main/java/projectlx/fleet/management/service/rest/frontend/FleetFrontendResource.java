package projectlx.fleet.management.service.rest.frontend;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import projectlx.fleet.management.service.processor.api.FleetAssetServiceProcessor;
import projectlx.fleet.management.service.processor.api.FleetComplianceServiceProcessor;
import projectlx.fleet.management.service.processor.api.FleetDriverServiceProcessor;
import projectlx.fleet.management.service.processor.api.FleetTrackingDeviceServiceProcessor;
import projectlx.fleet.management.service.processor.api.FleetTrackingIntegrationCredentialServiceProcessor;
import projectlx.fleet.management.utils.requests.AssignFleetAssetDriverRequest;
import projectlx.fleet.management.utils.requests.CreateFleetTrackingIntegrationCredentialRequest;
import projectlx.fleet.management.utils.requests.CompleteFleetAssetRegistrationRequest;
import projectlx.fleet.management.utils.requests.CreateFleetAssetRequest;
import projectlx.fleet.management.utils.requests.CreateFleetComplianceRecordRequest;
import projectlx.fleet.management.utils.requests.CreateFleetDriverRequest;
import projectlx.fleet.management.utils.requests.EditFleetAssetRequest;
import projectlx.fleet.management.utils.requests.EditFleetComplianceRecordRequest;
import projectlx.fleet.management.utils.requests.EditFleetDriverRequest;
import projectlx.fleet.management.utils.requests.EditFleetTrackingDeviceRequest;
import projectlx.fleet.management.utils.requests.InstallFleetTrackingDeviceRequest;
import projectlx.fleet.management.utils.responses.FleetAssetResponse;
import projectlx.fleet.management.utils.responses.FleetComplianceRecordResponse;
import projectlx.fleet.management.utils.responses.FleetDriverResponse;
import projectlx.fleet.management.utils.responses.FleetTrackingDeviceResponse;
import projectlx.fleet.management.utils.responses.FleetTrackingIntegrationCredentialResponse;
import projectlx.fleet.management.utils.security.FleetRoles;
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
    private final FleetTrackingDeviceServiceProcessor fleetTrackingDeviceServiceProcessor;
    private final FleetTrackingIntegrationCredentialServiceProcessor fleetTrackingIntegrationCredentialServiceProcessor;

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

    @Auditable(action = "ASSIGN_FLEET_ASSET_DRIVER")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/assets/{id}/assign-driver")
    @Operation(summary = "Assign fleet driver", description = "Assigns or clears the driver on a fleet asset without re-validating contract metadata.")
    public ResponseEntity<FleetAssetResponse> assignAssetDriverPost(
            @PathVariable final Long id,
            @RequestBody final AssignFleetAssetDriverRequest request,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        FleetAssetResponse response = fleetAssetServiceProcessor.assignDriver(id, request, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "ASSIGN_FLEET_ASSET_DRIVER")
    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/assets/{id}/driver")
    @Operation(summary = "Assign fleet driver", description = "Assigns or clears the driver on a fleet asset without re-validating contract metadata.")
    public ResponseEntity<FleetAssetResponse> assignAssetDriver(
            @PathVariable final Long id,
            @RequestBody final AssignFleetAssetDriverRequest request,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        FleetAssetResponse response = fleetAssetServiceProcessor.assignDriver(id, request, locale, username);
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

    @Auditable(action = "COMPLETE_FLEET_ASSET_REGISTRATION")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/assets/{id}/complete-registration")
    @Operation(
            summary = "Complete fleet asset registration",
            description = "Submits required compliance documents for the asset context "
                    + "(statutory, operating authority, hazardous cargo when tanker, lease when contracted, driver credentials when assigned) "
                    + "for a PENDING_COMPLIANCE asset and activates it.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fleet asset activated successfully"),
            @ApiResponse(responseCode = "400", description = "Asset not in PENDING_COMPLIANCE or missing required documents"),
            @ApiResponse(responseCode = "404", description = "Fleet asset not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public ResponseEntity<FleetAssetResponse> completeAssetRegistration(
            @PathVariable final Long id,
            @RequestBody final CompleteFleetAssetRegistrationRequest request,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        FleetAssetResponse response = fleetAssetServiceProcessor.completeRegistration(id, request, locale, username);
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

    @Auditable(action = "GET_MY_DRIVER_PROFILE")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/drivers/me")
    @Operation(summary = "Get my driver profile",
               description = "Returns the FleetDriver profile linked to the currently authenticated user's account.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Driver profile retrieved"),
            @ApiResponse(responseCode = "404", description = "No driver profile linked to this user"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public ResponseEntity<FleetDriverResponse> getMyDriverProfile(
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        FleetDriverResponse response = fleetDriverServiceProcessor.findMyProfile(locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "LIST_TRANSPORT_PARTNER_DRIVERS")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/transporter-partners/{transporterOrganizationId}/drivers")
    @Operation(summary = "List contracted transport partner drivers",
            description = "Returns the driver roster of a linked transport partner for assigning to contracted vehicles.")
    public ResponseEntity<FleetDriverResponse> listTransporterPartnerDrivers(
            @PathVariable final Long transporterOrganizationId,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        FleetDriverResponse response = fleetDriverServiceProcessor.listForTransporterPartner(
                transporterOrganizationId, locale, username);
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

    // ================================================================
    // DRIVER MARKETPLACE
    // ================================================================

    @Auditable(action = "SEARCH_DRIVER_MARKETPLACE")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/drivers/marketplace/search")
    @Operation(summary = "Search driver marketplace",
            description = "Returns marketplace-visible drivers not already employed by the caller's organisation. " +
                          "Optional filters: free-text term, licenseClass.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Marketplace drivers retrieved"),
            @ApiResponse(responseCode = "401", description = "Unauthorised")
    })
    public ResponseEntity<FleetDriverResponse> searchDriverMarketplace(
            @RequestParam(required = false) final String term,
            @RequestParam(required = false) final String licenseClass,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        FleetDriverResponse response = fleetDriverServiceProcessor.searchMarketplace(term, licenseClass, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "HIRE_FROM_DRIVER_MARKETPLACE")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/drivers/marketplace/{driverId}/hire")
    @Operation(summary = "Hire a driver from the marketplace",
            description = "Creates a POOL employment record for the driver under the caller's organisation.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Driver hired successfully"),
            @ApiResponse(responseCode = "404", description = "Driver not found or not marketplace-visible"),
            @ApiResponse(responseCode = "409", description = "Driver already employed by this organisation")
    })
    public ResponseEntity<FleetDriverResponse> hireDriverFromMarketplace(
            @PathVariable final Long driverId,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        FleetDriverResponse response = fleetDriverServiceProcessor.hireFromMarketplace(driverId, locale, username);
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

    // ================================================================
    // TRACKING DEVICES
    // ================================================================

    @Auditable(action = "LIST_TRACKING_DEVICES")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/tracking-devices")
    @Operation(summary = "List tracking devices", description = "Lists tracking devices for the signed-in user's organisation.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tracking devices retrieved"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public ResponseEntity<FleetTrackingDeviceResponse> listTrackingDevices(
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        FleetTrackingDeviceResponse response = fleetTrackingDeviceServiceProcessor.list(locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "INSTALL_TRACKING_DEVICE")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/tracking-devices")
    @Operation(summary = "Install tracking device", description = "Registers and activates a new tracking device for the organisation.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Tracking device installed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public ResponseEntity<FleetTrackingDeviceResponse> installTrackingDevice(
            @RequestBody final InstallFleetTrackingDeviceRequest request,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        FleetTrackingDeviceResponse response = fleetTrackingDeviceServiceProcessor.install(request, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "UPDATE_TRACKING_DEVICE")
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/tracking-devices/{id}")
    @Operation(summary = "Update tracking device", description = "Updates a tracking device by id.")
    public ResponseEntity<FleetTrackingDeviceResponse> updateTrackingDevice(
            @PathVariable final Long id,
            @RequestBody final EditFleetTrackingDeviceRequest request,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        FleetTrackingDeviceResponse response = fleetTrackingDeviceServiceProcessor.update(id, request, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "SUSPEND_TRACKING_DEVICE")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/tracking-devices/{id}/suspend")
    @Operation(summary = "Suspend tracking device", description = "Suspends a tracking device by id.")
    public ResponseEntity<FleetTrackingDeviceResponse> suspendTrackingDevice(
            @PathVariable final Long id,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        FleetTrackingDeviceResponse response = fleetTrackingDeviceServiceProcessor.suspend(id, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "DELETE_TRACKING_DEVICE")
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/tracking-devices/{id}")
    @Operation(summary = "Delete tracking device", description = "Soft-deletes a tracking device by id.")
    public ResponseEntity<FleetTrackingDeviceResponse> deleteTrackingDevice(
            @PathVariable final Long id,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        FleetTrackingDeviceResponse response = fleetTrackingDeviceServiceProcessor.delete(id, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    // ================================================================
    // TRACKING INTEGRATION CREDENTIALS (integrator ingest keys)
    // ================================================================

    @Auditable(action = "CREATE_TRACKING_INTEGRATION_CREDENTIAL")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/tracking-integration-credentials/create")
    @Operation(summary = "Create tracking integration credential",
               description = "Registers an integrator ingest key linked to a fleet vehicle.")
    public ResponseEntity<FleetTrackingIntegrationCredentialResponse> createTrackingIntegrationCredential(
            @Valid @RequestBody final CreateFleetTrackingIntegrationCredentialRequest request,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        FleetTrackingIntegrationCredentialResponse response =
                fleetTrackingIntegrationCredentialServiceProcessor.create(request, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/tracking-integration-credentials/find-by-organization/{organizationId}")
    @Operation(summary = "List tracking integration credentials for an organization")
    public ResponseEntity<FleetTrackingIntegrationCredentialResponse> listTrackingIntegrationCredentials(
            @PathVariable final Long organizationId,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        FleetTrackingIntegrationCredentialResponse response =
                fleetTrackingIntegrationCredentialServiceProcessor.findAllByOrganization(
                        organizationId, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/tracking-integration-credentials/find-by-id/{id}")
    @Operation(summary = "Find tracking integration credential by ID")
    public ResponseEntity<FleetTrackingIntegrationCredentialResponse> findTrackingIntegrationCredentialById(
            @PathVariable final Long id,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        FleetTrackingIntegrationCredentialResponse response =
                fleetTrackingIntegrationCredentialServiceProcessor.findById(id, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "SUSPEND_TRACKING_INTEGRATION_CREDENTIAL")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/tracking-integration-credentials/{id}/suspend")
    @Operation(summary = "Suspend a tracking integration credential")
    public ResponseEntity<FleetTrackingIntegrationCredentialResponse> suspendTrackingIntegrationCredential(
            @PathVariable final Long id,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        FleetTrackingIntegrationCredentialResponse response =
                fleetTrackingIntegrationCredentialServiceProcessor.suspend(id, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "DELETE_TRACKING_INTEGRATION_CREDENTIAL")
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/tracking-integration-credentials/delete/{id}")
    @Operation(summary = "Soft-delete a tracking integration credential")
    public ResponseEntity<FleetTrackingIntegrationCredentialResponse> deleteTrackingIntegrationCredential(
            @PathVariable final Long id,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        FleetTrackingIntegrationCredentialResponse response =
                fleetTrackingIntegrationCredentialServiceProcessor.delete(id, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }
}
