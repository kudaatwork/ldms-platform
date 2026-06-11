package projectlx.inventory.management.service.processor.impl;

import com.lowagie.text.DocumentException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import projectlx.inventory.management.business.logic.api.PurchaseOrderLineService;
import projectlx.inventory.management.service.processor.api.PurchaseOrderLineServiceProcessor;
import projectlx.inventory.management.utils.dtos.PurchaseOrderLineDto;
import projectlx.inventory.management.utils.dtos.ImportSummary;
import projectlx.inventory.management.utils.requests.CreatePurchaseOrderLineRequest;
import projectlx.inventory.management.utils.requests.EditPurchaseOrderLineRequest;
import projectlx.inventory.management.utils.requests.PurchaseOrderLineMultipleFiltersRequest;
import projectlx.inventory.management.utils.responses.PurchaseOrderLineResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PurchaseOrderLineServiceProcessorImpl implements PurchaseOrderLineServiceProcessor {

    private final PurchaseOrderLineService purchaseOrderLineService;
    private static final Logger logger = LoggerFactory.getLogger(PurchaseOrderLineServiceProcessorImpl.class);

    @Override
    public PurchaseOrderLineResponse create(CreatePurchaseOrderLineRequest request, Locale locale, String username) {
        logger.info("Incoming request to create purchase order line for user: {}", username);

        PurchaseOrderLineResponse response = purchaseOrderLineService.create(request, locale, username);

        logger.info("Outgoing response after creating purchase order line: Success: {}", 
                response != null ? response.isSuccess() : false);

        return response;
    }

    @Override
    public PurchaseOrderLineResponse findById(Long id, Locale locale, String username) {
        logger.info("Incoming request to find purchase order line by ID: {} for user: {}", id, username);

        PurchaseOrderLineResponse response = purchaseOrderLineService.findById(id, locale, username);

        logger.info("Outgoing response after finding purchase order line by ID: Success: {}", 
                response != null ? response.isSuccess() : false);

        return response;
    }

    @Override
    public PurchaseOrderLineResponse findAllAsList(Locale locale, String username) {
        logger.info("Incoming request to find all purchase order lines as list for user: {}", username);

        PurchaseOrderLineResponse response = purchaseOrderLineService.findAllAsList(locale, username);

        logger.info("Outgoing response after finding all purchase order lines: Success: {}", 
                response != null ? response.isSuccess() : false);

        return response;
    }

    @Override
    public PurchaseOrderLineResponse update(EditPurchaseOrderLineRequest request, String username, Locale locale) {
        logger.info("Incoming request to update purchase order line for user: {}", username);

        PurchaseOrderLineResponse response = purchaseOrderLineService.update(request, username, locale);

        logger.info("Outgoing response after updating purchase order line: Success: {}", 
                response != null ? response.isSuccess() : false);

        return response;
    }

    @Override
    public PurchaseOrderLineResponse delete(Long id, Locale locale, String username) {
        logger.info("Incoming request to delete purchase order line by ID: {} for user: {}", id, username);

        PurchaseOrderLineResponse response = purchaseOrderLineService.delete(id, locale, username);

        logger.info("Outgoing response after deleting purchase order line: Success: {}", 
                response != null ? response.isSuccess() : false);

        return response;
    }

    @Override
    public PurchaseOrderLineResponse findByMultipleFilters(PurchaseOrderLineMultipleFiltersRequest request, String username, Locale locale) {
        logger.info("Incoming request to find purchase order lines by multiple filters for user: {}", username);

        PurchaseOrderLineResponse response = purchaseOrderLineService.findByMultipleFilters(request, username, locale);

        logger.info("Outgoing response after finding purchase order lines by filters: Success: {}", 
                response != null ? response.isSuccess() : false);

        return response;
    }

    @Override
    public byte[] exportToCsv(List<PurchaseOrderLineDto> items) {
        logger.info("Incoming request to export {} purchase order lines to CSV", 
                items != null ? items.size() : 0);

        byte[] result = purchaseOrderLineService.exportToCsv(items);

        logger.info("Outgoing response after exporting to CSV: Size: {} bytes", 
                result != null ? result.length : 0);

        return result;
    }

    @Override
    public byte[] exportToExcel(List<PurchaseOrderLineDto> items) throws IOException {
        logger.info("Incoming request to export {} purchase order lines to Excel", 
                items != null ? items.size() : 0);

        byte[] result = purchaseOrderLineService.exportToExcel(items);

        logger.info("Outgoing response after exporting to Excel: Size: {} bytes", 
                result != null ? result.length : 0);

        return result;
    }

    @Override
    public byte[] exportToPdf(List<PurchaseOrderLineDto> items) throws DocumentException {
        logger.info("Incoming request to export {} purchase order lines to PDF", 
                items != null ? items.size() : 0);

        byte[] result = purchaseOrderLineService.exportToPdf(items);

        logger.info("Outgoing response after exporting to PDF: Size: {} bytes", 
                result != null ? result.length : 0);

        return result;
    }

    @Override
    public ImportSummary importPurchaseOrderLineFromCsv(InputStream csvInputStream) throws IOException {
        logger.info("Incoming request to import purchase order lines from CSV");

        ImportSummary result = purchaseOrderLineService.importPurchaseOrderLineFromCsv(csvInputStream);

        logger.info("Outgoing response after importing from CSV: Success: {}", 
                result != null);

        return result;
    }
}
