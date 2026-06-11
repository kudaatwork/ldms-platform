package projectlx.inventory.management.service.processor.impl;

import com.lowagie.text.DocumentException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import projectlx.inventory.management.business.logic.api.StockAdjustmentService;
import projectlx.inventory.management.service.processor.api.StockAdjustmentServiceProcessor;
import projectlx.inventory.management.utils.dtos.StockAdjustmentDto;
import projectlx.inventory.management.utils.dtos.ImportSummary;
import projectlx.inventory.management.utils.requests.CreateStockAdjustmentRequest;
import projectlx.inventory.management.utils.requests.EditStockAdjustmentRequest;
import projectlx.inventory.management.utils.requests.StockAdjustmentMultipleFiltersRequest;
import projectlx.inventory.management.utils.responses.StockAdjustmentResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class StockAdjustmentServiceProcessorImpl implements StockAdjustmentServiceProcessor {

    private final StockAdjustmentService stockAdjustmentService;
    private static final Logger logger = LoggerFactory.getLogger(StockAdjustmentServiceProcessorImpl.class);

    @Override
    public StockAdjustmentResponse create(CreateStockAdjustmentRequest request, Locale locale, String username) {
        logger.info("Incoming request to create stock adjustment for user: {}", username);

        StockAdjustmentResponse response = stockAdjustmentService.create(request, locale, username);

        logger.info("Outgoing response after creating stock adjustment: Success: {}",
                response != null ? response.isSuccess() : false);

        return response;
    }

    @Override
    public StockAdjustmentResponse findById(Long id, Locale locale, String username) {
        logger.info("Incoming request to find stock adjustment by ID: {} for user: {}", id, username);

        StockAdjustmentResponse response = stockAdjustmentService.findById(id, locale, username);

        logger.info("Outgoing response after finding stock adjustment by ID: Success: {}",
                response != null ? response.isSuccess() : false);

        return response;
    }

    @Override
    public StockAdjustmentResponse findAllAsList(Locale locale, String username) {
        logger.info("Incoming request to find all stock adjustments as list for user: {}", username);

        StockAdjustmentResponse response = stockAdjustmentService.findAllAsList(locale, username);

        logger.info("Outgoing response after finding all stock adjustments: Success: {}",
                response != null ? response.isSuccess() : false);

        return response;
    }

    @Override
    public StockAdjustmentResponse update(EditStockAdjustmentRequest request, String username, Locale locale) {
        logger.info("Incoming request to update stock adjustment for user: {}", username);

        StockAdjustmentResponse response = stockAdjustmentService.update(request, username, locale);

        logger.info("Outgoing response after updating stock adjustment: Success: {}",
                response != null ? response.isSuccess() : false);

        return response;
    }

    @Override
    public StockAdjustmentResponse delete(Long id, Locale locale, String username) {
        logger.info("Incoming request to delete stock adjustment by ID: {} for user: {}", id, username);

        StockAdjustmentResponse response = stockAdjustmentService.delete(id, locale, username);

        logger.info("Outgoing response after deleting stock adjustment: Success: {}",
                response != null ? response.isSuccess() : false);

        return response;
    }

    @Override
    public StockAdjustmentResponse findByMultipleFilters(StockAdjustmentMultipleFiltersRequest request, String username, Locale locale) {
        logger.info("Incoming request to find stock adjustments by multiple filters for user: {}", username);

        StockAdjustmentResponse response = stockAdjustmentService.findByMultipleFilters(request, username, locale);

        logger.info("Outgoing response after finding stock adjustments by filters: Success: {}",
                response != null ? response.isSuccess() : false);

        return response;
    }

    @Override
    public byte[] exportToCsv(List<StockAdjustmentDto> items) {
        logger.info("Incoming request to export {} stock adjustments to CSV",
                items != null ? items.size() : 0);

        byte[] result = stockAdjustmentService.exportToCsv(items);

        logger.info("Outgoing response after exporting to CSV: Size: {} bytes",
                result != null ? result.length : 0);

        return result;
    }

    @Override
    public byte[] exportToExcel(List<StockAdjustmentDto> items) throws IOException {
        logger.info("Incoming request to export {} stock adjustments to Excel",
                items != null ? items.size() : 0);

        byte[] result = stockAdjustmentService.exportToExcel(items);

        logger.info("Outgoing response after exporting to Excel: Size: {} bytes",
                result != null ? result.length : 0);

        return result;
    }

    @Override
    public byte[] exportToPdf(List<StockAdjustmentDto> items) throws DocumentException {
        logger.info("Incoming request to export {} stock adjustments to PDF",
                items != null ? items.size() : 0);

        byte[] result = stockAdjustmentService.exportToPdf(items);

        logger.info("Outgoing response after exporting to PDF: Size: {} bytes",
                result != null ? result.length : 0);

        return result;
    }

    @Override
    public ImportSummary importStockAdjustmentFromCsv(InputStream csvInputStream) throws IOException {
        logger.info("Incoming request to import stock adjustments from CSV");

        ImportSummary result = stockAdjustmentService.importStockAdjustmentFromCsv(csvInputStream);

        logger.info("Outgoing response after importing from CSV: Success: {}",
                result != null);

        return result;
    }
}
