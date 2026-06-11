package projectlx.inventory.management.business.logic.api;

import com.lowagie.text.DocumentException;
import projectlx.inventory.management.model.SalesOrderLine;
import projectlx.inventory.management.utils.dtos.SalesOrderLineDto;
import projectlx.inventory.management.utils.dtos.ImportSummary;
import projectlx.inventory.management.utils.requests.SalesOrderLineMultipleFiltersRequest;
import projectlx.inventory.management.utils.requests.CreateSalesOrderLineRequest;
import projectlx.inventory.management.utils.requests.EditSalesOrderLineRequest;
import projectlx.inventory.management.utils.responses.SalesOrderLineResponse;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public interface SalesOrderLineService {
    SalesOrderLineResponse create(CreateSalesOrderLineRequest request, Locale locale, String username);
    SalesOrderLineResponse findById(Long id, Locale locale, String username);
    Optional<SalesOrderLine> findSalesOrderLineById(Long id);
    SalesOrderLineResponse findAllAsList(Locale locale, String username);
    SalesOrderLineResponse update(EditSalesOrderLineRequest request, String username, Locale locale);
    SalesOrderLine updateFulfilledQuantity(Long salesOrderLineId, BigDecimal quantityFulfilled,
                                           Long fulfilledByUserId, Locale locale, String username);
    SalesOrderLineResponse delete(Long id, Locale locale, String username);
    SalesOrderLineResponse findByMultipleFilters(SalesOrderLineMultipleFiltersRequest request, String username, Locale locale);
    byte[] exportToCsv(List<SalesOrderLineDto> items);
    byte[] exportToExcel(List<SalesOrderLineDto> items) throws IOException;
    byte[] exportToPdf(List<SalesOrderLineDto> items) throws DocumentException;
    ImportSummary importSalesOrderLineFromCsv(InputStream csvInputStream) throws IOException;
}
