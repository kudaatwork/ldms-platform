package projectlx.trip.tracking.service.processor.api;

import projectlx.trip.tracking.utils.requests.FinishCountingRequest;
import projectlx.trip.tracking.utils.requests.RecordReturnLinesRequest;
import projectlx.trip.tracking.utils.requests.SendDeliveryOtpRequest;
import projectlx.trip.tracking.utils.requests.StartCountingRequest;
import projectlx.trip.tracking.utils.requests.VerifyDeliveryOtpRequest;
import projectlx.trip.tracking.utils.responses.TripDeliveryWorkflowResponse;

import java.util.Locale;

public interface TripDeliveryServiceProcessor {

    TripDeliveryWorkflowResponse getWorkflow(Long tripId, Locale locale, String username);

    TripDeliveryWorkflowResponse startCounting(Long tripId, StartCountingRequest request, Locale locale, String username);

    TripDeliveryWorkflowResponse finishCounting(Long tripId, FinishCountingRequest request, Locale locale, String username);

    TripDeliveryWorkflowResponse sendDeliveryOtp(SendDeliveryOtpRequest request, Locale locale, String username);

    TripDeliveryWorkflowResponse verifyDeliveryOtp(VerifyDeliveryOtpRequest request, Locale locale, String username);

    TripDeliveryWorkflowResponse startReturnJourney(Long tripId, Locale locale, String username);

    TripDeliveryWorkflowResponse recordReturns(Long tripId, RecordReturnLinesRequest request, Locale locale, String username);

    TripDeliveryWorkflowResponse confirmReturnComplete(Long tripId, Locale locale, String username);
}
