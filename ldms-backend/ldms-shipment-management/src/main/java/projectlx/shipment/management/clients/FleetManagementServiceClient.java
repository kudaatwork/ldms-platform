package projectlx.shipment.management.clients;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import projectlx.co.zw.shared_library.utils.constants.Constants;
import projectlx.shipment.management.utils.responses.FleetDriverFeignResponse;

import java.util.Locale;

public interface FleetManagementServiceClient {

    @GetMapping("/ldms-fleet-management/v1/system/fleet-driver/find-by-id/{id}")
    FleetDriverFeignResponse findFleetDriverById(
            @PathVariable("id") Long id,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale);
}
