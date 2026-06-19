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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;
import projectlx.fleet.management.service.processor.api.FleetTrackingIntegrationCredentialServiceProcessor;
import projectlx.fleet.management.utils.requests.CreateFleetTrackingIntegrationCredentialRequest;
import projectlx.fleet.management.utils.responses.FleetTrackingIntegrationCredentialResponse;

import java.util.Locale;

@CrossOrigin
@RestController
@RequestMapping("/ldms-fleet-management/v1/frontend/tracking-integration-credential")
@Tag(name = "Fleet Tracking Integration Credential Frontend Resource",
     description = "Manage ingest keys for third-party fleet tracking integrations")
@RequiredArgsConstructor
public class FleetTrackingIntegrationCredentialFrontendResource {

    private static final Logger logger =
            LoggerFactory.getLogger(FleetTrackingIntegrationCredentialFrontendResource.class);

    private final FleetTrackingIntegrationCredentialServiceProcessor processor;

    @Auditable(action = "CREATE_TRACKING_INTEGRATION_CREDENTIAL")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/create")
    @Operation(summary = "Create tracking integration credential",
               description = "Registers an integrator ingest key linked to a fleet vehicle.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Credential created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public ResponseEntity<FleetTrackingIntegrationCredentialResponse> create(
            @Valid @RequestBody final CreateFleetTrackingIntegrationCredentialRequest request,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
            final Locale locale) {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        FleetTrackingIntegrationCredentialResponse response = processor.create(request, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/find-by-organization/{organizationId}")
    @Operation(summary = "List tracking integration credentials for an organization")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Credentials retrieved"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<FleetTrackingIntegrationCredentialResponse> findAllByOrganization(
            @PathVariable final Long organizationId,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
            final Locale locale) {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        FleetTrackingIntegrationCredentialResponse response =
                processor.findAllByOrganization(organizationId, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/find-by-id/{id}")
    @Operation(summary = "Find tracking integration credential by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Credential retrieved"),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    public ResponseEntity<FleetTrackingIntegrationCredentialResponse> findById(
            @PathVariable final Long id,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
            final Locale locale) {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        FleetTrackingIntegrationCredentialResponse response = processor.findById(id, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "SUSPEND_TRACKING_INTEGRATION_CREDENTIAL")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{id}/suspend")
    @Operation(summary = "Suspend a tracking integration credential")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Credential suspended"),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    public ResponseEntity<FleetTrackingIntegrationCredentialResponse> suspend(
            @PathVariable final Long id,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
            final Locale locale) {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        FleetTrackingIntegrationCredentialResponse response = processor.suspend(id, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "DELETE_TRACKING_INTEGRATION_CREDENTIAL")
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/delete/{id}")
    @Operation(summary = "Soft-delete a tracking integration credential")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Credential deleted"),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    public ResponseEntity<FleetTrackingIntegrationCredentialResponse> delete(
            @PathVariable final Long id,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
            final Locale locale) {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        FleetTrackingIntegrationCredentialResponse response = processor.delete(id, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }
}
