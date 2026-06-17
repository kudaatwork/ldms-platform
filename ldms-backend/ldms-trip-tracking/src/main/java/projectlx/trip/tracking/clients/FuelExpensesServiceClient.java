package projectlx.trip.tracking.clients;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import projectlx.co.zw.shared_library.utils.constants.Constants;

import java.util.Locale;
import java.util.Map;

public interface FuelExpensesServiceClient {

    @PostMapping("/ldms-fuel-expenses/v1/system/fuel-session/location-updated")
    void notifyLocationUpdated(
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale);
}
