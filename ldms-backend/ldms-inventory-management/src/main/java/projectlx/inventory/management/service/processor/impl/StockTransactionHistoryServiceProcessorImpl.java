package projectlx.inventory.management.service.processor.impl;

import com.lowagie.text.DocumentException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import projectlx.inventory.management.business.logic.api.StockTransactionHistoryService;
import projectlx.inventory.management.service.processor.api.StockTransactionHistoryServiceProcessor;
import projectlx.inventory.management.utils.dtos.ImportSummary;
import projectlx.inventory.management.utils.dtos.StockTransactionHistoryDto;
import projectlx.inventory.management.utils.requests.CreateStockTransactionHistoryRequest;
import projectlx.inventory.management.utils.requests.EditStockTransactionHistoryRequest;
import projectlx.inventory.management.utils.requests.StockTransactionHistoryMultipleFiltersRequest;
import projectlx.inventory.management.utils.responses.StockTransactionHistoryResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class StockTransactionHistoryServiceProcessorImpl implements StockTransactionHistoryServiceProcessor {

    private final StockTransactionHistoryService stockTransactionHistoryService;
    private static final Logger logger = LoggerFactory.getLogger(StockTransactionHistoryServiceProcessorImpl.class);

    @Override
    public StockTransactionHistoryResponse create(CreateStockTransactionHistoryRequest request, Locale locale, String username) {
        logger.info("Incoming request to create a stock transaction history: {}", request);

        StockTransactionHistoryResponse response = stockTransactionHistoryService.create(request, locale, username);

        logger.info("Outgoing response after creating a stock transaction history: {}. Status Code: {}. Message: {}",
                response, response.getStatusCode(), response.getMessage());

        return response;
    }

    @Override
    public StockTransactionHistoryResponse findById(Long id, Locale locale, String username) {
        logger.info("Incoming request to find a stock transaction history by id: {}", id);

        StockTransactionHistoryResponse response = stockTransactionHistoryService.findById(id, locale, username);

        logger.info("Outgoing response after finding a stock transaction history by id: {}. Status Code: {}. Message: {}",
                response, response.getStatusCode(), response.getMessage());

        return response;
    }

    @Override
    public StockTransactionHistoryResponse findAllAsList(Locale locale, String username) {
        logger.info("Incoming request to find all stock transaction histories as a list");

        StockTransactionHistoryResponse response = stockTransactionHistoryService.findAllAsList(locale, username);

        logger.info("Outgoing response after finding all stock transaction histories as a list: {}. Status Code: {}. Message: {}",
                response, response.getStatusCode(), response.getMessage());

        return response;
    }

    @Override
    public StockTransactionHistoryResponse update(EditStockTransactionHistoryRequest request, String username, Locale locale) {
        logger.info("Incoming request to update a stock transaction history: {}", request);

        StockTransactionHistoryResponse response = stockTransactionHistoryService.update(request, username, locale);

        logger.info("Outgoing response after updating a stock transaction history: {}. Status Code: {}. Message: {}",
                response, response.getStatusCode(), response.getMessage());

        return response;
    }

    @Override
    public StockTransactionHistoryResponse delete(Long id, Locale locale, String username) {
        logger.info("Incoming request to delete a stock transaction history with the id: {}", id);

        StockTransactionHistoryResponse response = stockTransactionHistoryService.delete(id, locale, username);

        logger.info("Outgoing response after deleting a stock transaction history: {}. Status Code: {}. Message: {}",
                response, response.getStatusCode(), response.getMessage());

        return response;
    }

    @Override
    public StockTransactionHistoryResponse findByMultipleFilters(StockTransactionHistoryMultipleFiltersRequest request, String username, Locale locale) {
        logger.info("Incoming request to find stock transaction histories using multiple filters: {}", request);

        StockTransactionHistoryResponse response = stockTransactionHistoryService.findByMultipleFilters(request, username, locale);

        logger.info("Outgoing response after finding stock transaction histories using multiple filters: {}. Status Code: {}. Message: {}",
                response, response.getStatusCode(), response.getMessage());

        return response;
    }

    @Override
    public byte[] exportToCsv(List<StockTransactionHistoryDto> items) {
        logger.info("Incoming request to export stock transaction histories to CSV. Item count: {}", items.size());

        byte[] csvData = stockTransactionHistoryService.exportToCsv(items);

        logger.info("Outgoing CSV export complete. Byte size: {}", csvData.length);

        return csvData;
    }

    @Override
    public byte[] exportToExcel(List<StockTransactionHistoryDto> items) throws IOException {
        logger.info("Incoming request to export stock transaction histories to Excel. Item count: {}", items.size());

        byte[] excelData = stockTransactionHistoryService.exportToExcel(items);

        logger.info("Outgoing Excel export complete. Byte size: {}", excelData.length);

        return excelData;
    }

    @Override
    public byte[] exportToPdf(List<StockTransactionHistoryDto> items) throws DocumentException {
        logger.info("Incoming request to export stock transaction histories to PDF. Item count: {}", items.size());

        byte[] pdfData = stockTransactionHistoryService.exportToPdf(items);

        logger.info("Outgoing PDF export complete. Byte size: {}", pdfData.length);

        return pdfData;
    }

    @Override
    public ImportSummary importStockTransactionHistoryFromCsv(InputStream csvInputStream) throws IOException {
        logger.info("Incoming request to import stock transaction histories from CSV");

        ImportSummary importSummary = stockTransactionHistoryService.importStockTransactionHistoryFromCsv(csvInputStream);

        logger.info("Outgoing response after importing stock transaction histories from CSV: Total: {}, Success: {}, Failed: {}",
                importSummary.total, importSummary.success, importSummary.failed);

        return importSummary;
    }
}