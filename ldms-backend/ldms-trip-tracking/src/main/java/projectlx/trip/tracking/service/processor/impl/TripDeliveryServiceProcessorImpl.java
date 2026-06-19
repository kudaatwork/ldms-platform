package projectlx.trip.tracking.service.processor.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import projectlx.trip.tracking.business.logic.api.TripDeliveryService;
import projectlx.trip.tracking.service.processor.api.TripDeliveryServiceProcessor;
import projectlx.trip.tracking.utils.requests.FinishCountingRequest;
import projectlx.trip.tracking.utils.requests.RecordReturnLinesRequest;
import projectlx.trip.tracking.utils.requests.SendDeliveryOtpRequest;
import projectlx.trip.tracking.utils.requests.StartCountingRequest;
import projectlx.trip.tracking.utils.requests.VerifyDeliveryOtpRequest;
import projectlx.trip.tracking.utils.responses.TripDeliveryWorkflowResponse;

import java.util.Locale;

@RequiredArgsConstructor
@Slf4j
public class TripDeliveryServiceProcessorImpl implements TripDeliveryServiceProcessor {

    private final TripDeliveryService tripDeliveryService;

    @Override
    public TripDeliveryWorkflowResponse getWorkflow(Long tripId, Locale locale, String username) {
        log.info("Processing getWorkflow for trip {} by user {}", tripId, username);
        return tripDeliveryService.getWorkflow(tripId, locale, username);
    }

    @Override
    public TripDeliveryWorkflowResponse startCounting(Long tripId, StartCountingRequest request, Locale locale, String username) {
        log.info("Processing startCounting for trip {} actorRole={} by user {}", tripId,
                request != null ? request.getActorRole() : null, username);
        return tripDeliveryService.startCounting(tripId, request, locale, username);
    }

    @Override
    public TripDeliveryWorkflowResponse finishCounting(Long tripId, FinishCountingRequest request, Locale locale, String username) {
        log.info("Processing finishCounting for trip {} actorRole={} by user {}", tripId,
                request != null ? request.getActorRole() : null, username);
        return tripDeliveryService.finishCounting(tripId, request, locale, username);
    }

    @Override
    public TripDeliveryWorkflowResponse sendDeliveryOtp(SendDeliveryOtpRequest request, Locale locale, String username) {
        log.info("Processing sendDeliveryOtp for trip {} channel={} by user {}",
                request != null ? request.getTripId() : null,
                request != null ? request.getChannel() : null, username);
        return tripDeliveryService.sendDeliveryOtp(request, locale, username);
    }

    @Override
    public TripDeliveryWorkflowResponse verifyDeliveryOtp(VerifyDeliveryOtpRequest request, Locale locale, String username) {
        log.info("Processing verifyDeliveryOtp for trip {} by user {}",
                request != null ? request.getTripId() : null, username);
        return tripDeliveryService.verifyDeliveryOtp(request, locale, username);
    }

    @Override
    public TripDeliveryWorkflowResponse startReturnJourney(Long tripId, Locale locale, String username) {
        log.info("Processing startReturnJourney for trip {} by user {}", tripId, username);
        return tripDeliveryService.startReturnJourney(tripId, locale, username);
    }

    @Override
    public TripDeliveryWorkflowResponse recordReturns(Long tripId, RecordReturnLinesRequest request, Locale locale, String username) {
        log.info("Processing recordReturns for trip {} by user {}", tripId, username);
        return tripDeliveryService.recordReturns(tripId, request, locale, username);
    }

    @Override
    public TripDeliveryWorkflowResponse confirmReturnComplete(Long tripId, Locale locale, String username) {
        log.info("Processing confirmReturnComplete for trip {} by user {}", tripId, username);
        return tripDeliveryService.confirmReturnComplete(tripId, locale, username);
    }
}
