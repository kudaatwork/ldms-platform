package projectlx.inventory.management.service.processor.api;

import com.lowagie.text.DocumentException;
import projectlx.inventory.management.utils.dtos.InventoryItemDto;
import projectlx.inventory.management.utils.dtos.ImportSummary;
import projectlx.inventory.management.utils.requests.CreateInitialStockRequest;
import projectlx.inventory.management.utils.requests.InventoryItemMultipleFiltersRequest;
import projectlx.inventory.management.utils.requests.CreateInventoryItemRequest;
import projectlx.inventory.management.utils.requests.EditInventoryItemRequest;
import projectlx.inventory.management.utils.responses.InventoryItemResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

public interface InventoryItemServiceProcessor {
    InventoryItemResponse create(CreateInventoryItemRequest request, Locale locale, String username);
    InventoryItemResponse findById(Long id, Locale locale, String username);
    InventoryItemResponse findAllAsList(Locale locale, String username);
    InventoryItemResponse update(EditInventoryItemRequest request, String username, Locale locale);
    InventoryItemResponse delete(Long id, Locale locale, String username);
    InventoryItemResponse findByMultipleFilters(InventoryItemMultipleFiltersRequest request, String username, Locale locale);
    byte[] exportToCsv(List<InventoryItemDto> items);
    byte[] exportToExcel(List<InventoryItemDto> items) throws IOException;
    byte[] exportToPdf(List<InventoryItemDto> items) throws DocumentException;
    ImportSummary importInventoryItemFromCsv(InputStream csvInputStream) throws IOException;
    InventoryItemResponse createInitialStock(CreateInitialStockRequest request, Locale locale, String username);
    InventoryItemResponse createInitialStockBulk(List<CreateInitialStockRequest> requests, Locale locale, String username);
}
