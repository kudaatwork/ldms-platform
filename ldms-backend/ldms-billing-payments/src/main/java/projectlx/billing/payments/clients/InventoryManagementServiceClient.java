package projectlx.billing.payments.clients;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import projectlx.billing.payments.utils.requests.InventoryPurchaseOrderLineFilterRequest;
import projectlx.billing.payments.utils.responses.InventoryPurchaseOrderLineResponse;
import projectlx.billing.payments.utils.responses.InventoryPurchaseOrderResponse;
import projectlx.co.zw.shared_library.utils.constants.Constants;

import java.util.Locale;

public interface InventoryManagementServiceClient {

    @GetMapping("/ldms-inventory-management/v1/system/purchase-order/find-by-id/{id}")
    InventoryPurchaseOrderResponse findPurchaseOrderById(
            @PathVariable("id") Long id,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale);

    @PostMapping("/ldms-inventory-management/v1/system/purchase-order-line/find-by-multiple-filters")
    InventoryPurchaseOrderLineResponse findPurchaseOrderLinesByFilters(
            @RequestBody InventoryPurchaseOrderLineFilterRequest filters,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale);
}
