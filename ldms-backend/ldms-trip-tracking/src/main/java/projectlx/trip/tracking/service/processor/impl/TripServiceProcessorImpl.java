package projectlx.trip.tracking.service.processor.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import projectlx.trip.tracking.business.logic.api.TripService;
import projectlx.trip.tracking.service.processor.api.TripServiceProcessor;
import projectlx.trip.tracking.utils.requests.RecordLocationRequest;
import projectlx.trip.tracking.utils.requests.RecordTripEventRequest;
import projectlx.trip.tracking.utils.requests.StartTripRequest;
import projectlx.trip.tracking.utils.requests.TriggerArrivalRequest;
import projectlx.trip.tracking.utils.requests.TripFilterRequest;
import projectlx.trip.tracking.utils.requests.VerifyDeliveryOtpRequest;
import projectlx.trip.tracking.utils.responses.TripResponse;

import java.util.Locale;

@RequiredArgsConstructor
@Slf4j
public class TripServiceProcessorImpl implements TripServiceProcessor {

    private final TripService tripService;

    @Override
    public TripResponse startTrip(StartTripRequest request, Locale locale, String username) {
        log.info("Processing start trip for shipment {} by user {}", request != null ? request.getShipmentId() : null, username);
        return tripService.startTrip(request, locale, username);
    }

    @Override
    public TripResponse recordEvent(RecordTripEventRequest request, Locale locale, String username) {
        log.info("Processing record trip event for trip {} by user {}", request != null ? request.getTripId() : null, username);
        return tripService.recordEvent(request, locale, username);
    }

    @Override
    public TripResponse recordSystemEvent(RecordTripEventRequest request, Locale locale) {
        log.info("Processing system record trip event for trip {} event={}",
                request != null ? request.getTripId() : null,
                request != null ? request.getEventType() : null);
        return tripService.recordSystemEvent(request, locale);
    }

    @Override
    public TripResponse recordLocation(RecordLocationRequest request, Locale locale, String username) {
        log.info("Processing record location for trip {} by user {}", request != null ? request.getTripId() : null, username);
        return tripService.recordLocation(request, locale, username);
    }

    @Override
    public TripResponse triggerArrival(TriggerArrivalRequest request, Locale locale, String username) {
        log.info("Processing trigger arrival for trip {} by user {}", request != null ? request.getTripId() : null, username);
        return tripService.triggerArrival(request, locale, username);
    }

    @Override
    public TripResponse verifyDeliveryOtp(VerifyDeliveryOtpRequest request, Locale locale, String username) {
        log.info("Processing verify delivery OTP for trip {} by user {}", request != null ? request.getTripId() : null, username);
        return tripService.verifyDeliveryOtp(request, locale, username);
    }

    @Override
    public TripResponse findById(Long id, Locale locale, String username) {
        log.info("Processing find trip by id {} for user {}", id, username);
        return tripService.findById(id, locale, username);
    }

    @Override
    public TripResponse findByMultipleFilters(TripFilterRequest request, Locale locale, String username) {
        log.info("Processing find trips by filters for user {}", username);
        return tripService.findByMultipleFilters(request, locale, username);
    }

    @Override
    public TripResponse track(Long id, Locale locale) {
        log.info("Processing public track for trip id {}", id);
        return tripService.track(id, locale);
    }

    @Override
    public TripResponse listMyTrips(Locale locale, String username) {
        log.info("Processing list my trips for user {}", username);
        return tripService.listMyTrips(locale, username);
    }

    @Override
    public TripResponse findMyTripById(Long tripId, Locale locale, String username) {
        log.info("Processing find my trip {} for user {}", tripId, username);
        return tripService.findMyTripById(tripId, locale, username);
    }

    @Override
    public TripResponse getMyTripMetrics(Locale locale, String username) {
        log.info("Processing my trip metrics for user {}", username);
        return tripService.getMyTripMetrics(locale, username);
    }
}
