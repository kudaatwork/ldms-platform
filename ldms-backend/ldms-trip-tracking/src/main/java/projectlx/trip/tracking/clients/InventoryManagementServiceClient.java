package projectlx.trip.tracking.clients;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import projectlx.trip.tracking.utils.dtos.InventoryCompleteSalesOrderWithGrvDto;
import projectlx.trip.tracking.utils.dtos.InventoryCompleteWithGrvDto;
import projectlx.trip.tracking.utils.dtos.InventoryRouteStopListFeignResponse;
import projectlx.trip.tracking.utils.dtos.InventoryStartSalesOrderDispatchDto;
import projectlx.trip.tracking.utils.dtos.InventoryStartTransitDto;
import projectlx.co.zw.shared_library.utils.constants.Constants;

import java.util.Locale;

public interface InventoryManagementServiceClient {

    @PostMapping("/ldms-inventory-management/v1/system/inventory-transfer/start-transit")
    void startTransit(
            @RequestBody InventoryStartTransitDto request,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale);

    @PostMapping("/ldms-inventory-management/v1/system/inventory-transfer/complete-with-grv")
    void completeWithGrv(
            @RequestBody InventoryCompleteWithGrvDto request,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale);

    @PostMapping("/ldms-inventory-management/v1/system/sales-order/start-dispatch")
    void startSalesOrderDispatch(
            @RequestBody InventoryStartSalesOrderDispatchDto request,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale);

    @PostMapping("/ldms-inventory-management/v1/system/sales-order/complete-with-grv")
    void completeSalesOrderWithGrv(
            @RequestBody InventoryCompleteSalesOrderWithGrvDto request,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale);

    @GetMapping("/ldms-inventory-management/v1/system/logistics-route-stop/find-by-context")
    InventoryRouteStopListFeignResponse findRouteStopsByContext(
            @RequestParam String contextType,
            @RequestParam Long contextId,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale);
}
