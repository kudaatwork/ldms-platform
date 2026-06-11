package projectlx.inventory.management.service.processor.api;

import com.lowagie.text.DocumentException;
import projectlx.inventory.management.utils.dtos.PurchaseOrderLineDto;
import projectlx.inventory.management.utils.dtos.ImportSummary;
import projectlx.inventory.management.utils.requests.PurchaseOrderLineMultipleFiltersRequest;
import projectlx.inventory.management.utils.requests.CreatePurchaseOrderLineRequest;
import projectlx.inventory.management.utils.requests.EditPurchaseOrderLineRequest;
import projectlx.inventory.management.utils.responses.PurchaseOrderLineResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

public interface PurchaseOrderLineServiceProcessor {
    PurchaseOrderLineResponse create(CreatePurchaseOrderLineRequest request, Locale locale, String username);
    PurchaseOrderLineResponse findById(Long id, Locale locale, String username);
    PurchaseOrderLineResponse findAllAsList(Locale locale, String username);
    PurchaseOrderLineResponse update(EditPurchaseOrderLineRequest request, String username, Locale locale);
    PurchaseOrderLineResponse delete(Long id, Locale locale, String username);
    PurchaseOrderLineResponse findByMultipleFilters(PurchaseOrderLineMultipleFiltersRequest request, String username, Locale locale);
    byte[] exportToCsv(List<PurchaseOrderLineDto> items);
    byte[] exportToExcel(List<PurchaseOrderLineDto> items) throws IOException;
    byte[] exportToPdf(List<PurchaseOrderLineDto> items) throws DocumentException;
    ImportSummary importPurchaseOrderLineFromCsv(InputStream csvInputStream) throws IOException;
}
