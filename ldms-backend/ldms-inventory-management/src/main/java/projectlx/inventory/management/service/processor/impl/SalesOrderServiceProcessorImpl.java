package projectlx.inventory.management.service.processor.impl;

import com.lowagie.text.DocumentException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import projectlx.inventory.management.business.logic.api.SalesOrderService;
import projectlx.inventory.management.model.SalesOrder;
import projectlx.inventory.management.service.processor.api.SalesOrderServiceProcessor;
import projectlx.inventory.management.utils.dtos.SalesOrderDto;
import projectlx.inventory.management.utils.dtos.ImportSummary;
import projectlx.inventory.management.utils.requests.CreateSalesOrderRequest;
import projectlx.inventory.management.utils.requests.EditSalesOrderRequest;
import projectlx.inventory.management.utils.requests.FulfillSalesOrderRequest;
import projectlx.inventory.management.utils.requests.SalesOrderMultipleFiltersRequest;
import projectlx.inventory.management.utils.responses.SalesOrderResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class SalesOrderServiceProcessorImpl implements SalesOrderServiceProcessor {

    private final SalesOrderService salesOrderService;
    private static final Logger logger = LoggerFactory.getLogger(SalesOrderServiceProcessorImpl.class);

    @Override
    public SalesOrderResponse create(CreateSalesOrderRequest request, Locale locale, String username) {
        logger.info("Incoming request to create sales order for user: {}", username);

        SalesOrderResponse response = salesOrderService.create(request, locale, username);

        logger.info("Outgoing response after creating sales order: Success: {}",
                response != null ? response.isSuccess() : false);

        return response;
    }

    @Override
    public SalesOrderResponse findById(Long id, Locale locale, String username) {
        logger.info("Incoming request to find sales order by ID: {} for user: {}", id, username);

        SalesOrderResponse response = salesOrderService.findById(id, locale, username);

        logger.info("Outgoing response after finding sales order by ID: Success: {}",
                response != null ? response.isSuccess() : false);

        return response;
    }

    @Override
    public SalesOrderResponse findAllAsList(Locale locale, String username) {
        logger.info("Incoming request to find all sales orders as list for user: {}", username);

        SalesOrderResponse response = salesOrderService.findAllAsList(locale, username);

        logger.info("Outgoing response after finding all sales orders: Success: {}",
                response != null ? response.isSuccess() : false);

        return response;
    }

    @Override
    public SalesOrderResponse update(EditSalesOrderRequest request, String username, Locale locale) {
        logger.info("Incoming request to update sales order for user: {}", username);

        SalesOrderResponse response = salesOrderService.update(request, username, locale);

        logger.info("Outgoing response after updating sales order: Success: {}",
                response != null ? response.isSuccess() : false);

        return response;
    }

    @Override
    public SalesOrderResponse fulfillOrder(FulfillSalesOrderRequest request, String username, Locale locale) {
        logger.info("Incoming request to fulfill sales order for user: {}", username);

        SalesOrderResponse response = salesOrderService.fulfillOrder(request, username, locale);

        logger.info("Outgoing response after fulfilling sales order: Success: {}",
                response != null ? response.isSuccess() : false);

        return response;
    }

    @Override
    public SalesOrderResponse delete(Long id, Locale locale, String username) {
        logger.info("Incoming request to delete sales order by ID: {} for user: {}", id, username);

        SalesOrderResponse response = salesOrderService.delete(id, locale, username);

        logger.info("Outgoing response after deleting sales order: Success: {}",
                response != null ? response.isSuccess() : false);

        return response;
    }

    @Override
    public SalesOrderResponse getFinancialSummary(Long salesOrderId, Long organizationId, Locale locale, String username) {
        logger.info("Incoming request to get sales order financial summary for ID: {} and org: {}", salesOrderId, organizationId);
        return salesOrderService.getFinancialSummary(salesOrderId, organizationId, locale, username);
    }

    @Override
    public SalesOrderResponse findByMultipleFilters(SalesOrderMultipleFiltersRequest request, String username, Locale locale) {
        logger.info("Incoming request to find sales orders by multiple filters for user: {}", username);

        SalesOrderResponse response = salesOrderService.findByMultipleFilters(request, username, locale);

        logger.info("Outgoing response after finding sales orders by filters: Success: {}",
                response != null ? response.isSuccess() : false);

        return response;
    }

    @Override
    public void allocateInventoryForOrder(SalesOrder salesOrder, Locale locale, String username) {
        logger.info("Incoming request to allocate inventory for sales order for user: {}", username);

        salesOrderService.allocateInventoryForOrder(salesOrder, locale, username);

        logger.info("Outgoing response after allocating inventory for sales order: Success: {}",
                salesOrder != null ? salesOrder.getSalesOrderNumber() : false);
    }

    @Override
    public void releaseInventoryReservations(SalesOrder salesOrder, Locale locale, String username) {
        logger.info("Incoming request to release inventory reservations for sales order for user: {}", username);

        salesOrderService.releaseInventoryReservations(salesOrder, locale, username);

        logger.info("Outgoing response after releasing inventory reservations for sales order: Success: {}",
                salesOrder != null ? salesOrder.getSalesOrderNumber() : false);
    }

    @Override
    public byte[] exportToCsv(List<SalesOrderDto> items) {
        logger.info("Incoming request to export {} sales orders to CSV",
                items != null ? items.size() : 0);

        byte[] result = salesOrderService.exportToCsv(items);

        logger.info("Outgoing response after exporting to CSV: Size: {} bytes",
                result != null ? result.length : 0);

        return result;
    }

    @Override
    public byte[] exportToExcel(List<SalesOrderDto> items) throws IOException {
        logger.info("Incoming request to export {} sales orders to Excel",
                items != null ? items.size() : 0);

        byte[] result = salesOrderService.exportToExcel(items);

        logger.info("Outgoing response after exporting to Excel: Size: {} bytes",
                result != null ? result.length : 0);

        return result;
    }

    @Override
    public byte[] exportToPdf(List<SalesOrderDto> items) throws DocumentException {
        logger.info("Incoming request to export {} sales orders to PDF",
                items != null ? items.size() : 0);

        byte[] result = salesOrderService.exportToPdf(items);

        logger.info("Outgoing response after exporting to PDF: Size: {} bytes",
                result != null ? result.length : 0);

        return result;
    }

    @Override
    public ImportSummary importSalesOrderFromCsv(InputStream csvInputStream) throws IOException {
        logger.info("Incoming request to import sales orders from CSV");

        ImportSummary result = salesOrderService.importSalesOrderFromCsv(csvInputStream);

        logger.info("Outgoing response after importing from CSV: Success: {}",
                result != null);

        return result;
    }
}
