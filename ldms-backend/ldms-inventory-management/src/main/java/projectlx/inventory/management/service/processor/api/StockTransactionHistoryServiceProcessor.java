package projectlx.inventory.management.service.processor.api;

import com.lowagie.text.DocumentException;
import projectlx.inventory.management.utils.dtos.StockTransactionHistoryDto;
import projectlx.inventory.management.utils.dtos.ImportSummary;
import projectlx.inventory.management.utils.requests.StockTransactionHistoryMultipleFiltersRequest;
import projectlx.inventory.management.utils.requests.CreateStockTransactionHistoryRequest;
import projectlx.inventory.management.utils.requests.EditStockTransactionHistoryRequest;
import projectlx.inventory.management.utils.responses.StockTransactionHistoryResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

public interface StockTransactionHistoryServiceProcessor {
    StockTransactionHistoryResponse create(CreateStockTransactionHistoryRequest request, Locale locale, String username);
    StockTransactionHistoryResponse findById(Long id, Locale locale, String username);
    StockTransactionHistoryResponse findAllAsList(Locale locale, String username);
    StockTransactionHistoryResponse update(EditStockTransactionHistoryRequest request, String username, Locale locale);
    StockTransactionHistoryResponse delete(Long id, Locale locale, String username);
    StockTransactionHistoryResponse findByMultipleFilters(StockTransactionHistoryMultipleFiltersRequest request, String username, Locale locale);
    byte[] exportToCsv(List<StockTransactionHistoryDto> items);
    byte[] exportToExcel(List<StockTransactionHistoryDto> items) throws IOException;
    byte[] exportToPdf(List<StockTransactionHistoryDto> items) throws DocumentException;
    ImportSummary importStockTransactionHistoryFromCsv(InputStream csvInputStream) throws IOException;
}