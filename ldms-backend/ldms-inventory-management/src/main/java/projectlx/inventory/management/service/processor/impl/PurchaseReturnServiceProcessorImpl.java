package projectlx.inventory.management.service.processor.impl;

import com.lowagie.text.DocumentException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import projectlx.inventory.management.business.logic.api.PurchaseReturnService;
import projectlx.inventory.management.service.processor.api.PurchaseReturnServiceProcessor;
import projectlx.inventory.management.utils.dtos.ImportSummary;
import projectlx.inventory.management.utils.dtos.PurchaseReturnDto;
import projectlx.inventory.management.utils.requests.CreatePurchaseReturnRequest;
import projectlx.inventory.management.utils.requests.EditPurchaseReturnRequest;
import projectlx.inventory.management.utils.requests.PurchaseReturnMultipleFiltersRequest;
import projectlx.inventory.management.utils.responses.PurchaseReturnResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PurchaseReturnServiceProcessorImpl implements PurchaseReturnServiceProcessor {

    private final PurchaseReturnService purchaseReturnService;
    private static final Logger logger = LoggerFactory.getLogger(PurchaseReturnServiceProcessorImpl.class);

    @Override
    public PurchaseReturnResponse create(CreatePurchaseReturnRequest request, Locale locale, String username) {
        logger.info("Incoming request to create a purchase return: {}", request);

        PurchaseReturnResponse response = purchaseReturnService.create(request, locale, username);

        logger.info("Outgoing response after creating a purchase return: {}. Status Code: {}. Message: {}",
                response, response.getStatusCode(), response.getMessage());

        return response;
    }

    @Override
    public PurchaseReturnResponse findById(Long id, Locale locale, String username) {
        logger.info("Incoming request to find a purchase return by id: {}", id);

        PurchaseReturnResponse response = purchaseReturnService.findById(id, locale, username);

        logger.info("Outgoing response after finding a purchase return by id: {}. Status Code: {}. Message: {}",
                response, response.getStatusCode(), response.getMessage());

        return response;
    }

    @Override
    public PurchaseReturnResponse findAllAsList(Locale locale, String username) {
        logger.info("Incoming request to find all purchase returns as a list");

        PurchaseReturnResponse response = purchaseReturnService.findAllAsList(locale, username);

        logger.info("Outgoing response after finding all purchase returns as a list: {}. Status Code: {}. Message: {}",
                response, response.getStatusCode(), response.getMessage());

        return response;
    }

    @Override
    public PurchaseReturnResponse update(EditPurchaseReturnRequest request, String username, Locale locale) {
        logger.info("Incoming request to update a purchase return: {}", request);

        PurchaseReturnResponse response = purchaseReturnService.update(request, username, locale);

        logger.info("Outgoing response after updating a purchase return: {}. Status Code: {}. Message: {}",
                response, response.getStatusCode(), response.getMessage());

        return response;
    }

    @Override
    public PurchaseReturnResponse delete(Long id, Locale locale, String username) {
        logger.info("Incoming request to delete a purchase return with the id: {}", id);

        PurchaseReturnResponse response = purchaseReturnService.delete(id, locale, username);

        logger.info("Outgoing response after deleting a purchase return: {}. Status Code: {}. Message: {}",
                response, response.getStatusCode(), response.getMessage());

        return response;
    }

    @Override
    public PurchaseReturnResponse findByMultipleFilters(PurchaseReturnMultipleFiltersRequest request, String username, Locale locale) {
        logger.info("Incoming request to find purchase returns using multiple filters: {}", request);

        PurchaseReturnResponse response = purchaseReturnService.findByMultipleFilters(request, username, locale);

        logger.info("Outgoing response after finding purchase returns using multiple filters: {}. Status Code: {}. Message: {}",
                response, response.getStatusCode(), response.getMessage());

        return response;
    }

    @Override
    public byte[] exportToCsv(List<PurchaseReturnDto> items) {
        logger.info("Incoming request to export purchase returns to CSV. Item count: {}", items.size());

        byte[] csvData = purchaseReturnService.exportToCsv(items);

        logger.info("Outgoing CSV export complete. Byte size: {}", csvData.length);

        return csvData;
    }

    @Override
    public byte[] exportToExcel(List<PurchaseReturnDto> items) throws IOException {
        logger.info("Incoming request to export purchase returns to Excel. Item count: {}", items.size());

        byte[] excelData = purchaseReturnService.exportToExcel(items);

        logger.info("Outgoing Excel export complete. Byte size: {}", excelData.length);

        return excelData;
    }

    @Override
    public byte[] exportToPdf(List<PurchaseReturnDto> items) throws DocumentException {
        logger.info("Incoming request to export purchase returns to PDF. Item count: {}", items.size());

        byte[] pdfData = purchaseReturnService.exportToPdf(items);

        logger.info("Outgoing PDF export complete. Byte size: {}", pdfData.length);

        return pdfData;
    }

    @Override
    public ImportSummary importPurchaseReturnFromCsv(InputStream csvInputStream) throws IOException {
        logger.info("Incoming request to import purchase returns from CSV");

        ImportSummary importSummary = purchaseReturnService.importPurchaseReturnFromCsv(csvInputStream);

        logger.info("Outgoing response after importing purchase returns from CSV: Total: {}, Success: {}, Failed: {}",
                importSummary.total, importSummary.success, importSummary.failed);

        return importSummary;
    }
}