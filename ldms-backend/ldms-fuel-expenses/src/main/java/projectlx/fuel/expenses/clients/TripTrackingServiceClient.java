package projectlx.fuel.expenses.clients;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import projectlx.co.zw.shared_library.utils.constants.Constants;
import projectlx.fuel.expenses.utils.requests.RecordTripEventFeignRequest;

import java.util.Locale;
import java.util.Map;

public interface TripTrackingServiceClient {

    /**
     * Records a trip event via the trip-tracking system endpoint.
     * Used by fuel-expenses to post roadside stop and resume events.
     */
    @PostMapping("/ldms-trip-tracking/v1/system/trip/record-event")
    Map<String, Object> recordTripEvent(
            @RequestBody RecordTripEventFeignRequest request,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale);
}
