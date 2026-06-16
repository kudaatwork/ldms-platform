package projectlx.fuel.expenses.service.rest.frontend;

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
import projectlx.co.zw.shared_library.utils.constants.Constants;
import projectlx.fuel.expenses.service.processor.api.OperationalFundRequestServiceProcessor;
import projectlx.fuel.expenses.utils.requests.ApproveFundRequestRequest;
import projectlx.fuel.expenses.utils.requests.CancelFundRequestRequest;
import projectlx.fuel.expenses.utils.requests.CreateFundRequestRequest;
import projectlx.fuel.expenses.utils.requests.FundRequestFilterRequest;
import projectlx.fuel.expenses.utils.requests.RejectFundRequestRequest;
import projectlx.fuel.expenses.utils.responses.OperationalFundRequestResponse;
import java.util.Locale;

@CrossOrigin
@RestController
@RequestMapping("/ldms-fuel-expenses/v1/frontend/operational-fund-request")
@Tag(name = "Operational Fund Request Frontend Resource",
        description = "Driver fuel top-up and funds requests during a trip")
@RequiredArgsConstructor
public class OperationalFundRequestFrontendResource {

    private static final Logger logger = LoggerFactory.getLogger(OperationalFundRequestFrontendResource.class);

    private final OperationalFundRequestServiceProcessor operationalFundRequestServiceProcessor;

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/create")
    @Operation(summary = "Create a fund request",
            description = "Driver creates a FUEL_TOP_UP or FUNDS request during an active trip.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Fund request created"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<OperationalFundRequestResponse> create(
            @RequestBody CreateFundRequestRequest request,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
            Locale locale) {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        logger.info("POST /operational-fund-request/create tripId={} by user={}", request.getTripId(), username);

        OperationalFundRequestResponse response =
                operationalFundRequestServiceProcessor.create(request, locale, username);

        if (!response.isSuccess()) {
            int status = response.getStatusCode() > 0 ? response.getStatusCode() : 400;
            return ResponseEntity.status(status).body(response);
        }
        return ResponseEntity.status(201).body(response);
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/approve")
    @Operation(summary = "Approve a fund request",
            description = "Approves a PENDING fund request. For FUEL_TOP_UP, applies litres to the active fuel session.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fund request approved"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "404", description = "Fund request not found"),
            @ApiResponse(responseCode = "409", description = "Fund request is not in PENDING status"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<OperationalFundRequestResponse> approve(
            @RequestBody ApproveFundRequestRequest request,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
            Locale locale) {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        logger.info("POST /operational-fund-request/approve id={} by user={}", request.getRequestId(), username);

        OperationalFundRequestResponse response =
                operationalFundRequestServiceProcessor.approve(request, locale, username);

        if (!response.isSuccess()) {
            int status = response.getStatusCode() > 0 ? response.getStatusCode() : 400;
            return ResponseEntity.status(status).body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/reject")
    @Operation(summary = "Reject a fund request",
            description = "Rejects a PENDING fund request with a mandatory reason.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fund request rejected"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "404", description = "Fund request not found"),
            @ApiResponse(responseCode = "409", description = "Fund request is not in PENDING status"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<OperationalFundRequestResponse> reject(
            @RequestBody RejectFundRequestRequest request,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
            Locale locale) {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        logger.info("POST /operational-fund-request/reject id={} by user={}", request.getRequestId(), username);

        OperationalFundRequestResponse response =
                operationalFundRequestServiceProcessor.reject(request, locale, username);

        if (!response.isSuccess()) {
            int status = response.getStatusCode() > 0 ? response.getStatusCode() : 400;
            return ResponseEntity.status(status).body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/cancel")
    @Operation(summary = "Cancel a fund request",
            description = "Cancels a PENDING fund request (driver-initiated).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fund request cancelled"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "404", description = "Fund request not found"),
            @ApiResponse(responseCode = "409", description = "Fund request is not in PENDING status"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<OperationalFundRequestResponse> cancel(
            @RequestBody CancelFundRequestRequest request,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
            Locale locale) {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        logger.info("POST /operational-fund-request/cancel id={} by user={}", request.getRequestId(), username);

        OperationalFundRequestResponse response =
                operationalFundRequestServiceProcessor.cancel(request, locale, username);

        if (!response.isSuccess()) {
            int status = response.getStatusCode() > 0 ? response.getStatusCode() : 400;
            return ResponseEntity.status(status).body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/find-by-multiple-filters")
    @Operation(summary = "Search fund requests by filters",
            description = "Returns a paginated list of fund requests matching the supplied filter criteria.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fund requests returned"),
            @ApiResponse(responseCode = "400", description = "Invalid filter request"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<OperationalFundRequestResponse> findByMultipleFilters(
            @RequestBody FundRequestFilterRequest request,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
            Locale locale) {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        logger.info("POST /operational-fund-request/find-by-multiple-filters by user={}", username);

        OperationalFundRequestResponse response =
                operationalFundRequestServiceProcessor.findByMultipleFilters(request, locale, username);

        if (!response.isSuccess()) {
            int status = response.getStatusCode() > 0 ? response.getStatusCode() : 400;
            return ResponseEntity.status(status).body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/find-by-id/{id}")
    @Operation(summary = "Find a fund request by ID",
            description = "Returns the fund request with the specified ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fund request returned"),
            @ApiResponse(responseCode = "400", description = "Invalid ID"),
            @ApiResponse(responseCode = "404", description = "Fund request not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<OperationalFundRequestResponse> findById(
            @PathVariable Long id,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
            Locale locale) {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        logger.info("GET /operational-fund-request/find-by-id/{} by user={}", id, username);

        OperationalFundRequestResponse response =
                operationalFundRequestServiceProcessor.findById(id, locale, username);

        if (!response.isSuccess()) {
            int status = response.getStatusCode() > 0 ? response.getStatusCode() : 400;
            return ResponseEntity.status(status).body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/complete-roadside/{tripId}")
    @Operation(summary = "Complete a roadside stop",
            description = "Records a ROADSIDE_RESUMED event on the trip, transitioning it from ROADSIDE_HOLD " +
                    "back to IN_TRANSIT. Call this after fuel fill or mechanic work is finished.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Roadside stop completed, trip resumed"),
            @ApiResponse(responseCode = "400", description = "Invalid trip ID"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<OperationalFundRequestResponse> completeRoadsideStop(
            @PathVariable Long tripId,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
            Locale locale) {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        logger.info("POST /operational-fund-request/complete-roadside/{} by user={}", tripId, username);

        OperationalFundRequestResponse response =
                operationalFundRequestServiceProcessor.completeRoadsideStop(tripId, locale, username);

        if (!response.isSuccess()) {
            int status = response.getStatusCode() > 0 ? response.getStatusCode() : 400;
            return ResponseEntity.status(status).body(response);
        }
        return ResponseEntity.ok(response);
    }
}
