package projectlx.inventory.management.clients;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import projectlx.co.zw.shared_library.utils.constants.Constants;
import projectlx.inventory.management.clients.dto.ShipmentFeignResponse;

import java.util.Locale;

public interface ShipmentManagementServiceClient {

    @GetMapping("/ldms-shipment-management/v1/system/shipment/by-transfer/{transferId}")
    ShipmentFeignResponse findByTransferId(
            @PathVariable("transferId") Long transferId,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale);

    @GetMapping("/ldms-shipment-management/v1/system/shipment/by-sales-order/{salesOrderId}")
    ShipmentFeignResponse findBySalesOrderId(
            @PathVariable("salesOrderId") Long salesOrderId,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale);
}
