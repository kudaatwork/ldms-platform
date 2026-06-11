package projectlx.inventory.management.service.processor.api;

import com.lowagie.text.DocumentException;
import projectlx.inventory.management.utils.dtos.SalesOrderLineDto;
import projectlx.inventory.management.utils.dtos.ImportSummary;
import projectlx.inventory.management.utils.requests.SalesOrderLineMultipleFiltersRequest;
import projectlx.inventory.management.utils.requests.CreateSalesOrderLineRequest;
import projectlx.inventory.management.utils.requests.EditSalesOrderLineRequest;
import projectlx.inventory.management.utils.responses.SalesOrderLineResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

public interface SalesOrderLineServiceProcessor {
    SalesOrderLineResponse create(CreateSalesOrderLineRequest request, Locale locale, String username);
    SalesOrderLineResponse findById(Long id, Locale locale, String username);
    SalesOrderLineResponse findAllAsList(Locale locale, String username);
    SalesOrderLineResponse update(EditSalesOrderLineRequest request, String username, Locale locale);
    SalesOrderLineResponse delete(Long id, Locale locale, String username);
    SalesOrderLineResponse findByMultipleFilters(SalesOrderLineMultipleFiltersRequest request, String username, Locale locale);
    byte[] exportToCsv(List<SalesOrderLineDto> items);
    byte[] exportToExcel(List<SalesOrderLineDto> items) throws IOException;
    byte[] exportToPdf(List<SalesOrderLineDto> items) throws DocumentException;
    ImportSummary importSalesOrderLineFromCsv(InputStream csvInputStream) throws IOException;
}
