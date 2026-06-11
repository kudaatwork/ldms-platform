package projectlx.trip.tracking.clients;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import projectlx.trip.tracking.utils.requests.UpdateShipmentStatusFeignRequest;
import projectlx.trip.tracking.utils.responses.ShipmentFeignResponse;
import projectlx.co.zw.shared_library.utils.constants.Constants;

import java.util.Locale;

public interface ShipmentManagementServiceClient {

    @GetMapping("/ldms-shipment-management/v1/system/shipment/find-by-id/{id}")
    ShipmentFeignResponse findShipmentById(
            @PathVariable("id") Long id,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale);

    @PatchMapping("/ldms-shipment-management/v1/system/shipment/status")
    ShipmentFeignResponse updateShipmentStatus(
            @RequestBody UpdateShipmentStatusFeignRequest request,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale);
}
