package projectlx.shipment.management.clients;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import projectlx.shipment.management.utils.requests.RecordTripSystemEventFeignRequest;
import projectlx.shipment.management.utils.responses.TripSystemEventFeignResponse;
import projectlx.co.zw.shared_library.utils.constants.Constants;

import java.util.Locale;

public interface TripTrackingServiceClient {

    @PostMapping("/ldms-trip-tracking/v1/system/trip/record-event")
    TripSystemEventFeignResponse recordSystemEvent(
            @RequestBody RecordTripSystemEventFeignRequest request,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale);
}
