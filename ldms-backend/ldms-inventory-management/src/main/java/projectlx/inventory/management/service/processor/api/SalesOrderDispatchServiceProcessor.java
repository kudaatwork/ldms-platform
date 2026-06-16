package projectlx.inventory.management.service.processor.api;

import projectlx.inventory.management.utils.requests.CompleteSalesOrderWithGrvRequest;
import projectlx.inventory.management.utils.requests.StartSalesOrderDispatchRequest;
import projectlx.inventory.management.utils.responses.SalesOrderResponse;

import java.util.Locale;

public interface SalesOrderDispatchServiceProcessor {

    SalesOrderResponse startDispatch(StartSalesOrderDispatchRequest request, Locale locale, String username);

    SalesOrderResponse completeWithGrv(CompleteSalesOrderWithGrvRequest request, Locale locale, String username);
}
