package projectlx.fleet.management.clients;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import projectlx.co.zw.shared_library.utils.constants.Constants;
import projectlx.fleet.management.utils.requests.AutoAllocateShipmentFromFleetRequest;
import projectlx.fleet.management.utils.responses.ShipmentFeignResponse;

import java.util.Locale;

public interface ShipmentManagementServiceClient {

    @PostMapping("/ldms-shipment-management/v1/system/shipment/auto-allocate-from-fleet")
    ShipmentFeignResponse autoAllocateFromFleet(
            @RequestBody AutoAllocateShipmentFromFleetRequest request,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale);
}
