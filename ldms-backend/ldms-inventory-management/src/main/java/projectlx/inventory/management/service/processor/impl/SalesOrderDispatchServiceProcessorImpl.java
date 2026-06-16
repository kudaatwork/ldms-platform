package projectlx.inventory.management.service.processor.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import projectlx.inventory.management.business.logic.api.SalesOrderDispatchService;
import projectlx.inventory.management.service.processor.api.SalesOrderDispatchServiceProcessor;
import projectlx.inventory.management.utils.requests.CompleteSalesOrderWithGrvRequest;
import projectlx.inventory.management.utils.requests.StartSalesOrderDispatchRequest;
import projectlx.inventory.management.utils.responses.SalesOrderResponse;

import java.util.Locale;

@RequiredArgsConstructor
@Slf4j
public class SalesOrderDispatchServiceProcessorImpl implements SalesOrderDispatchServiceProcessor {

    private final SalesOrderDispatchService salesOrderDispatchService;

    @Override
    public SalesOrderResponse startDispatch(StartSalesOrderDispatchRequest request, Locale locale, String username) {
        log.info("Processing start dispatch for sales order {}", request != null ? request.getSalesOrderId() : null);
        return salesOrderDispatchService.startDispatch(
                request.getSalesOrderId(),
                request.getStartedByUserId(),
                request.getTripId(),
                request.getShipmentId(),
                locale,
                username);
    }

    @Override
    public SalesOrderResponse completeWithGrv(CompleteSalesOrderWithGrvRequest request, Locale locale, String username) {
        log.info("Processing complete-with-grv for sales order {}", request != null ? request.getSalesOrderId() : null);
        return salesOrderDispatchService.completeWithGrv(
                request.getSalesOrderId(),
                request.getReceivedByUserId(),
                request.getIdempotencyKey(),
                locale,
                username);
    }
}
