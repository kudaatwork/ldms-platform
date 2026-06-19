package projectlx.trip.tracking.service.rest.frontend;

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
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;
import projectlx.trip.tracking.service.processor.api.TripDeliveryServiceProcessor;
import projectlx.trip.tracking.utils.requests.FinishCountingRequest;
import projectlx.trip.tracking.utils.requests.RecordReturnLinesRequest;
import projectlx.trip.tracking.utils.requests.SendDeliveryOtpRequest;
import projectlx.trip.tracking.utils.requests.StartCountingRequest;
import projectlx.trip.tracking.utils.requests.VerifyDeliveryOtpRequest;
import projectlx.trip.tracking.utils.responses.TripDeliveryWorkflowResponse;

import java.util.Locale;

@CrossOrigin
@RestController
@RequestMapping("/ldms-trip-tracking/v1/frontend/trip-delivery")
@Tag(name = "Trip Delivery Frontend Resource", description = "Multi-step delivery workflow: stock counting, OTP verification, returns")
@RequiredArgsConstructor
public class TripDeliveryFrontendResource {

    private static final Logger logger = LoggerFactory.getLogger(TripDeliveryFrontendResource.class);

    private final TripDeliveryServiceProcessor tripDeliveryServiceProcessor;

    @Auditable(action = "GET_DELIVERY_WORKFLOW")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{tripId}")
    @Operation(summary = "Get delivery workflow", description = "Returns the current delivery workflow state for a trip.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Workflow retrieved"),
            @ApiResponse(responseCode = "404", description = "Workflow or trip not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public ResponseEntity<TripDeliveryWorkflowResponse> getWorkflow(
            @PathVariable final Long tripId,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        TripDeliveryWorkflowResponse response = tripDeliveryServiceProcessor.getWorkflow(tripId, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "START_STOCK_COUNTING")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{tripId}/start-counting")
    @Operation(summary = "Start stock counting",
               description = "Starts stock counting for the given actor (DRIVER | CUSTOMER | RECEIVER). "
                       + "Trip transitions to COUNTING_STOCK when the first actor begins.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Counting started"),
            @ApiResponse(responseCode = "400", description = "Trip not in correct state or already counting"),
            @ApiResponse(responseCode = "404", description = "Trip not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public ResponseEntity<TripDeliveryWorkflowResponse> startCounting(
            @PathVariable final Long tripId,
            @RequestBody final StartCountingRequest request,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        TripDeliveryWorkflowResponse response = tripDeliveryServiceProcessor.startCounting(tripId, request, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "FINISH_STOCK_COUNTING")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{tripId}/finish-counting")
    @Operation(summary = "Finish stock counting",
               description = "Marks counting done for the actor. When both parties finish, trip moves to COUNT_COMPLETE.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Counting finished"),
            @ApiResponse(responseCode = "400", description = "Trip not in COUNTING_STOCK state"),
            @ApiResponse(responseCode = "404", description = "Trip or workflow not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public ResponseEntity<TripDeliveryWorkflowResponse> finishCounting(
            @PathVariable final Long tripId,
            @RequestBody final FinishCountingRequest request,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        TripDeliveryWorkflowResponse response = tripDeliveryServiceProcessor.finishCounting(tripId, request, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "SEND_DELIVERY_OTP")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/send-otp")
    @Operation(summary = "Send delivery OTP",
               description = "Generates and sends delivery OTP via the specified channel (SMS | WHATSAPP | EMAIL). "
                       + "Trip must be COUNT_COMPLETE. Transitions trip to OTP_PENDING.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OTP sent successfully"),
            @ApiResponse(responseCode = "400", description = "Trip not COUNT_COMPLETE or invalid request"),
            @ApiResponse(responseCode = "404", description = "Trip not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public ResponseEntity<TripDeliveryWorkflowResponse> sendDeliveryOtp(
            @RequestBody final SendDeliveryOtpRequest request,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        TripDeliveryWorkflowResponse response = tripDeliveryServiceProcessor.sendDeliveryOtp(request, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "VERIFY_DELIVERY_OTP_WORKFLOW")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/verify-otp")
    @Operation(summary = "Verify delivery OTP (workflow)",
               description = "Verifies the OTP, completes GRV, marks trip DELIVERED. Accepts optional delivery notes.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Delivery verified"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired OTP"),
            @ApiResponse(responseCode = "404", description = "Trip not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public ResponseEntity<TripDeliveryWorkflowResponse> verifyDeliveryOtp(
            @RequestBody final VerifyDeliveryOtpRequest request,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        TripDeliveryWorkflowResponse response = tripDeliveryServiceProcessor.verifyDeliveryOtp(request, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "START_RETURN_JOURNEY")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{tripId}/start-return")
    @Operation(summary = "Start return journey",
               description = "Initiates the return journey for a delivered trip. Transitions DELIVERED → RETURN_IN_TRANSIT.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Return journey started"),
            @ApiResponse(responseCode = "400", description = "Trip not in DELIVERED state"),
            @ApiResponse(responseCode = "404", description = "Trip not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public ResponseEntity<TripDeliveryWorkflowResponse> startReturnJourney(
            @PathVariable final Long tripId,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        TripDeliveryWorkflowResponse response = tripDeliveryServiceProcessor.startReturnJourney(tripId, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "RECORD_RETURN_LINES")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{tripId}/record-returns")
    @Operation(summary = "Record return lines",
               description = "Records stock items being returned with quantities and reasons.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Return lines recorded"),
            @ApiResponse(responseCode = "400", description = "Invalid request or trip not in return transit"),
            @ApiResponse(responseCode = "404", description = "Trip not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public ResponseEntity<TripDeliveryWorkflowResponse> recordReturns(
            @PathVariable final Long tripId,
            @RequestBody final RecordReturnLinesRequest request,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        TripDeliveryWorkflowResponse response = tripDeliveryServiceProcessor.recordReturns(tripId, request, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "CONFIRM_RETURN_COMPLETE")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{tripId}/confirm-return")
    @Operation(summary = "Confirm return complete",
               description = "Confirms the return journey is complete. Transitions RETURN_IN_TRANSIT → RETURNED.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Return confirmed"),
            @ApiResponse(responseCode = "400", description = "Trip not in RETURN_IN_TRANSIT state"),
            @ApiResponse(responseCode = "404", description = "Trip not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public ResponseEntity<TripDeliveryWorkflowResponse> confirmReturnComplete(
            @PathVariable final Long tripId,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        TripDeliveryWorkflowResponse response = tripDeliveryServiceProcessor.confirmReturnComplete(tripId, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }
}
