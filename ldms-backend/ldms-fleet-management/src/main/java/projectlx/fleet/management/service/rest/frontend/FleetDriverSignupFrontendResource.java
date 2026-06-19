package projectlx.fleet.management.service.rest.frontend;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.responses.FileUploadResponse;
import projectlx.fleet.management.service.processor.api.FleetDriverSignupRequestServiceProcessor;
import projectlx.fleet.management.utils.enums.DriverSignupDocumentSlot;
import projectlx.fleet.management.utils.requests.CreateFleetDriverSignupRequest;
import projectlx.fleet.management.utils.responses.FleetDriverSignupRequestResponse;
import projectlx.co.zw.shared_library.utils.constants.Constants;

import java.util.Locale;

/**
 * Driver self-service signup and transporter-side signup management endpoint.
 * The submit endpoint is unauthenticated; list/approve/reject require TRANSPORTER_ADMIN role.
 */
@CrossOrigin
@RestController
@RequestMapping("/ldms-fleet-management/v1/frontend/fleet/drivers")
@Tag(name = "Fleet Driver Signup Resource", description = "Driver signup request management")
@RequiredArgsConstructor
public class FleetDriverSignupFrontendResource {

    private static final Logger logger = LoggerFactory.getLogger(FleetDriverSignupFrontendResource.class);

    private final FleetDriverSignupRequestServiceProcessor fleetDriverSignupRequestServiceProcessor;

    @PostMapping(value = "/signup-request/document-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Upload a driver signup identity document",
            description = "Public staging upload for national ID or licence (front/back). " +
                          "Use the same stagingSessionId on submit. No authentication required.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Document uploaded"),
            @ApiResponse(responseCode = "400", description = "Invalid upload")
    })
    public ResponseEntity<FileUploadResponse> uploadSignupDocument(
            @RequestParam Long stagingSessionId,
            @RequestParam DriverSignupDocumentSlot documentSlot,
            @RequestPart("file") MultipartFile file,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                    defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        FileUploadResponse response = fleetDriverSignupRequestServiceProcessor.uploadSignupDocument(
                stagingSessionId, documentSlot, file, locale);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @PostMapping("/signup-request")
    @Operation(
            summary = "Submit a driver signup request",
            description = "Allows a prospective driver to submit a signup request. " +
                          "Omit companyCode for a freelance signup (enters marketplace). " +
                          "No authentication required.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Signup request submitted successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data or duplicate pending email"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public ResponseEntity<FleetDriverSignupRequestResponse> submitSignupRequest(
            @RequestBody final CreateFleetDriverSignupRequest request,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                    defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        logger.info("Public driver signup request received for email={}, companyCode={}",
                request != null ? request.getEmail() : null,
                request != null ? request.getCompanyCode() : null);
        FleetDriverSignupRequestResponse response =
                fleetDriverSignupRequestServiceProcessor.submitSignupRequest(request, locale);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @GetMapping("/signup-requests/pending")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List pending COMPANY signup requests for caller organisation")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pending signup requests retrieved"),
            @ApiResponse(responseCode = "401", description = "Unauthorised")
    })
    public ResponseEntity<FleetDriverSignupRequestResponse> listPendingForOrganization(
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                    defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        FleetDriverSignupRequestResponse response =
                fleetDriverSignupRequestServiceProcessor.listPendingForOrganization(locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @GetMapping("/signup-requests/freelance-marketplace")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List all pending FREELANCE signup requests (marketplace pool)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Freelance signup requests retrieved"),
            @ApiResponse(responseCode = "401", description = "Unauthorised")
    })
    public ResponseEntity<FleetDriverSignupRequestResponse> listFreelanceMarketplace(
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                    defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        FleetDriverSignupRequestResponse response =
                fleetDriverSignupRequestServiceProcessor.listFreelanceMarketplace(locale);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "APPROVE_DRIVER_SIGNUP_REQUEST")
    @PostMapping("/signup-requests/{id}/approve")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Approve a driver signup request",
            description = "Approves a PENDING signup request, provisions driver + platform access.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Signup request approved"),
            @ApiResponse(responseCode = "404", description = "Signup request not found"),
            @ApiResponse(responseCode = "400", description = "Not in PENDING state")
    })
    public ResponseEntity<FleetDriverSignupRequestResponse> approveSignupRequest(
            @PathVariable Long id,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                    defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        FleetDriverSignupRequestResponse response =
                fleetDriverSignupRequestServiceProcessor.approveSignupRequest(id, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "REJECT_DRIVER_SIGNUP_REQUEST")
    @PostMapping("/signup-requests/{id}/reject")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Reject a driver signup request")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Signup request rejected"),
            @ApiResponse(responseCode = "404", description = "Signup request not found"),
            @ApiResponse(responseCode = "400", description = "Not in PENDING state")
    })
    public ResponseEntity<FleetDriverSignupRequestResponse> rejectSignupRequest(
            @PathVariable Long id,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                    defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        FleetDriverSignupRequestResponse response =
                fleetDriverSignupRequestServiceProcessor.rejectSignupRequest(id, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }
}
