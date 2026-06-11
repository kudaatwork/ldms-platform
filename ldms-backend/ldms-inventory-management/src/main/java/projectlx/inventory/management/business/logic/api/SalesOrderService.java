package projectlx.inventory.management.business.logic.api;

import com.lowagie.text.DocumentException;
import projectlx.inventory.management.model.SalesOrder;
import projectlx.inventory.management.utils.dtos.SalesOrderDto;
import projectlx.inventory.management.utils.dtos.ImportSummary;
import projectlx.inventory.management.utils.requests.FulfillSalesOrderRequest;
import projectlx.inventory.management.utils.requests.SalesOrderMultipleFiltersRequest;
import projectlx.inventory.management.utils.requests.CreateSalesOrderRequest;
import projectlx.inventory.management.utils.requests.EditSalesOrderRequest;
import projectlx.inventory.management.utils.responses.SalesOrderResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

public interface SalesOrderService {
    SalesOrderResponse create(CreateSalesOrderRequest request, Locale locale, String username);
    SalesOrderResponse findById(Long id, Locale locale, String username);
    SalesOrderResponse findAllAsList(Locale locale, String username);
    SalesOrderResponse update(EditSalesOrderRequest request, String username, Locale locale);
    SalesOrderResponse fulfillOrder(FulfillSalesOrderRequest request, String username, Locale locale);
    SalesOrderResponse delete(Long id, Locale locale, String username);
    SalesOrderResponse getFinancialSummary(Long salesOrderId, Long organizationId, Locale locale, String username);
    SalesOrderResponse findByMultipleFilters(SalesOrderMultipleFiltersRequest request, String username, Locale locale);
    void allocateInventoryForOrder(SalesOrder salesOrder, Locale locale, String username);
    void releaseInventoryReservations(SalesOrder salesOrder, Locale locale, String username);
    byte[] exportToCsv(List<SalesOrderDto> items);
    byte[] exportToExcel(List<SalesOrderDto> items) throws IOException;
    byte[] exportToPdf(List<SalesOrderDto> items) throws DocumentException;
    ImportSummary importSalesOrderFromCsv(InputStream csvInputStream) throws IOException;
}
