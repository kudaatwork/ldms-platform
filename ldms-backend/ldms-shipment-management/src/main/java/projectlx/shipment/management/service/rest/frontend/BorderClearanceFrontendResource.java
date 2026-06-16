package projectlx.shipment.management.service.rest.frontend;

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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import projectlx.shipment.management.service.processor.api.BorderClearanceCaseServiceProcessor;
import projectlx.shipment.management.utils.requests.AddBorderClearanceDocumentRequest;
import projectlx.shipment.management.utils.requests.BorderClearanceMultipleFiltersRequest;
import projectlx.shipment.management.utils.requests.ClearBorderCaseRequest;
import projectlx.shipment.management.utils.requests.RejectBorderCaseRequest;
import projectlx.shipment.management.utils.responses.BorderClearanceCaseResponse;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;

import java.util.Locale;

@CrossOrigin
@RestController
@RequestMapping("/ldms-shipment-management/v1/frontend/border-clearance")
@Tag(name = "Border Clearance Frontend Resource", description = "Cross-border clearance case management for frontend consumers")
@RequiredArgsConstructor
public class BorderClearanceFrontendResource {

    private static final Logger logger = LoggerFactory.getLogger(BorderClearanceFrontendResource.class);

    private final BorderClearanceCaseServiceProcessor borderClearanceCaseServiceProcessor;

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/find-by-id/{id}")
    @Operation(summary = "Find border clearance case by ID",
            description = "Returns a border clearance case with all its uploaded documents.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Case found"),
            @ApiResponse(responseCode = "404", description = "Case not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public ResponseEntity<BorderClearanceCaseResponse> findById(
            @PathVariable final Long id,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        BorderClearanceCaseResponse response = borderClearanceCaseServiceProcessor.findById(id, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/find-by-multiple-filters")
    @Operation(summary = "Search border clearance cases",
            description = "Filter cases by shipmentId, status, and/or organizationId.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cases retrieved"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public ResponseEntity<BorderClearanceCaseResponse> findByMultipleFilters(
            @RequestBody final BorderClearanceMultipleFiltersRequest request,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        BorderClearanceCaseResponse response = borderClearanceCaseServiceProcessor.findByMultipleFilters(request, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "BORDER_CLEARANCE_ADD_DOCUMENT")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/add-document")
    @Operation(summary = "Attach a document to a border clearance case",
            description = "Links a file upload (by fileUploadId) to the case with a document type.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Document attached"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "404", description = "Case not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public ResponseEntity<BorderClearanceCaseResponse> addDocument(
            @RequestBody final AddBorderClearanceDocumentRequest request,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        BorderClearanceCaseResponse response = borderClearanceCaseServiceProcessor.addDocument(request, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "BORDER_CLEARANCE_SUBMIT")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/submit/{id}")
    @Operation(summary = "Submit a border clearance case for review",
            description = "Transitions case from AWAITING_DOCUMENTS → SUBMITTED.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Case submitted"),
            @ApiResponse(responseCode = "400", description = "Invalid status transition"),
            @ApiResponse(responseCode = "404", description = "Case not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public ResponseEntity<BorderClearanceCaseResponse> submit(
            @PathVariable final Long id,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        BorderClearanceCaseResponse response = borderClearanceCaseServiceProcessor.submit(id, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "BORDER_CLEARANCE_CLEAR")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/clear/{id}")
    @Operation(summary = "Mark a border clearance case as cleared",
            description = "Clears the case. If a trip is linked, notifies trip-tracking with a BORDER_CLEARED event.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Case cleared"),
            @ApiResponse(responseCode = "400", description = "Invalid status transition"),
            @ApiResponse(responseCode = "404", description = "Case not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public ResponseEntity<BorderClearanceCaseResponse> clear(
            @PathVariable final Long id,
            @RequestBody(required = false) final ClearBorderCaseRequest request,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        BorderClearanceCaseResponse response = borderClearanceCaseServiceProcessor.clear(id, request, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "BORDER_CLEARANCE_REJECT")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/reject/{id}")
    @Operation(summary = "Reject a border clearance case",
            description = "Transitions the case to REJECTED with optional rejection notes.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Case rejected"),
            @ApiResponse(responseCode = "400", description = "Invalid status transition"),
            @ApiResponse(responseCode = "404", description = "Case not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public ResponseEntity<BorderClearanceCaseResponse> reject(
            @PathVariable final Long id,
            @RequestBody(required = false) final RejectBorderCaseRequest request,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        BorderClearanceCaseResponse response = borderClearanceCaseServiceProcessor.reject(id, request, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }
}
