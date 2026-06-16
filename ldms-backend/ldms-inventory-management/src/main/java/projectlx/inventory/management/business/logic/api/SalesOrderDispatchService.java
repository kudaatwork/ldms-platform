package projectlx.inventory.management.business.logic.api;

import projectlx.inventory.management.utils.responses.SalesOrderResponse;

import java.util.Locale;

public interface SalesOrderDispatchService {

    /**
     * Starts bought-goods dispatch when a trip departs: reserves stock and moves it to transit.
     * Callable only by trip-tracking (SYSTEM).
     */
    SalesOrderResponse startDispatch(Long salesOrderId, Long startedByUserId, Long tripId, Long shipmentId,
                                     Locale locale, String username);

    /**
     * Completes customer delivery after OTP verification: PO-linked GRV at receiving warehouse.
     */
    SalesOrderResponse completeWithGrv(Long salesOrderId, Long receivedByUserId, String idempotencyKey,
                                       Locale locale, String username);
}
