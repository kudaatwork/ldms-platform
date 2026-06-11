package projectlx.inventory.management.business.logic.api;

import com.lowagie.text.DocumentException;
import projectlx.inventory.management.utils.dtos.StockAdjustmentDto;
import projectlx.inventory.management.utils.dtos.ImportSummary;
import projectlx.inventory.management.utils.requests.StockAdjustmentMultipleFiltersRequest;
import projectlx.inventory.management.utils.requests.CreateStockAdjustmentRequest;
import projectlx.inventory.management.utils.requests.EditStockAdjustmentRequest;
import projectlx.inventory.management.utils.responses.StockAdjustmentResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

public interface StockAdjustmentService {
    StockAdjustmentResponse create(CreateStockAdjustmentRequest request, Locale locale, String username);
    StockAdjustmentResponse findById(Long id, Locale locale, String username);
    StockAdjustmentResponse findAllAsList(Locale locale, String username);
    StockAdjustmentResponse update(EditStockAdjustmentRequest request, String username, Locale locale);
    StockAdjustmentResponse delete(Long id, Locale locale, String username);
    StockAdjustmentResponse findByMultipleFilters(StockAdjustmentMultipleFiltersRequest request, String username, Locale locale);
    byte[] exportToCsv(List<StockAdjustmentDto> items);
    byte[] exportToExcel(List<StockAdjustmentDto> items) throws IOException;
    byte[] exportToPdf(List<StockAdjustmentDto> items) throws DocumentException;
    ImportSummary importStockAdjustmentFromCsv(InputStream csvInputStream) throws IOException;
}
