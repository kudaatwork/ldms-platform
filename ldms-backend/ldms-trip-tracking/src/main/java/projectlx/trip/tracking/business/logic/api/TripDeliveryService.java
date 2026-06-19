package projectlx.trip.tracking.business.logic.api;

import projectlx.trip.tracking.utils.requests.FinishCountingRequest;
import projectlx.trip.tracking.utils.requests.RecordReturnLinesRequest;
import projectlx.trip.tracking.utils.requests.SendDeliveryOtpRequest;
import projectlx.trip.tracking.utils.requests.VerifyDeliveryOtpRequest;
import projectlx.trip.tracking.utils.requests.StartCountingRequest;
import projectlx.trip.tracking.utils.responses.TripDeliveryWorkflowResponse;

import java.util.Locale;

public interface TripDeliveryService {

    /** Retrieves the delivery workflow state for a trip. */
    TripDeliveryWorkflowResponse getWorkflow(Long tripId, Locale locale, String username);

    /**
     * Starts stock counting for the given actor role (DRIVER | CUSTOMER | RECEIVER).
     * The trip transitions to COUNTING_STOCK when the first actor begins.
     */
    TripDeliveryWorkflowResponse startCounting(Long tripId, StartCountingRequest request, Locale locale, String username);

    /**
     * Marks counting as finished for the given actor.
     * When both driver and customer have finished, the trip transitions to COUNT_COMPLETE.
     */
    TripDeliveryWorkflowResponse finishCounting(Long tripId, FinishCountingRequest request, Locale locale, String username);

    /**
     * Sends the delivery OTP via the specified channel (SMS | WHATSAPP | EMAIL).
     * Transitions the trip from COUNT_COMPLETE → OTP_PENDING.
     */
    TripDeliveryWorkflowResponse sendDeliveryOtp(SendDeliveryOtpRequest request, Locale locale, String username);

    /**
     * Verifies the delivery OTP and completes the delivery.
     * Delegates GRV creation and trip → DELIVERED transition.
     * Accepts optional delivery notes.
     */
    TripDeliveryWorkflowResponse verifyDeliveryOtp(VerifyDeliveryOtpRequest request, Locale locale, String username);

    /**
     * Starts the return journey for delivered trip.
     * Transitions DELIVERED → RETURN_IN_TRANSIT.
     */
    TripDeliveryWorkflowResponse startReturnJourney(Long tripId, Locale locale, String username);

    /**
     * Records return line items (product, quantity, reason) for a trip in return transit.
     */
    TripDeliveryWorkflowResponse recordReturns(Long tripId, RecordReturnLinesRequest request, Locale locale, String username);

    /**
     * Confirms the return journey is complete.
     * Transitions RETURN_IN_TRANSIT → RETURNED.
     */
    TripDeliveryWorkflowResponse confirmReturnComplete(Long tripId, Locale locale, String username);
}
