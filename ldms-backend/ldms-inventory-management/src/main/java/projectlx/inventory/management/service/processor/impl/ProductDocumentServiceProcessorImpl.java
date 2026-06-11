package projectlx.inventory.management.service.processor.impl;

import com.lowagie.text.DocumentException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import projectlx.inventory.management.business.logic.api.ProductDocumentService;
import projectlx.inventory.management.service.processor.api.ProductDocumentServiceProcessor;
import projectlx.inventory.management.utils.dtos.ProductDocumentDto;
import projectlx.inventory.management.utils.dtos.ImportSummary;
import projectlx.inventory.management.utils.requests.CreateProductDocumentRequest;
import projectlx.inventory.management.utils.requests.EditProductDocumentRequest;
import projectlx.inventory.management.utils.requests.ProductDocumentMultipleFiltersRequest;
import projectlx.inventory.management.utils.responses.ProductDocumentResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ProductDocumentServiceProcessorImpl implements ProductDocumentServiceProcessor {

    private final ProductDocumentService productDocumentService;
    private static final Logger logger = LoggerFactory.getLogger(ProductDocumentServiceProcessorImpl.class);

    @Override
    public ProductDocumentResponse create(CreateProductDocumentRequest request, Locale locale, String username) {
        logger.info("Incoming request to create product document for user: {}", username);

        ProductDocumentResponse response = productDocumentService.create(request, locale, username);

        logger.info("Outgoing response after creating product document: Success: {}", 
                response != null ? response.isSuccess() : false);

        return response;
    }

    @Override
    public ProductDocumentResponse findById(Long id, Locale locale, String username) {
        logger.info("Incoming request to find product document by ID: {} for user: {}", id, username);

        ProductDocumentResponse response = productDocumentService.findById(id, locale, username);

        logger.info("Outgoing response after finding product document by ID: Success: {}", 
                response != null ? response.isSuccess() : false);

        return response;
    }

    @Override
    public ProductDocumentResponse findAllAsList(Locale locale, String username) {
        logger.info("Incoming request to find all product documents as list for user: {}", username);

        ProductDocumentResponse response = productDocumentService.findAllAsList(locale, username);

        logger.info("Outgoing response after finding all product documents: Success: {}", 
                response != null ? response.isSuccess() : false);

        return response;
    }

    @Override
    public ProductDocumentResponse update(EditProductDocumentRequest request, String username, Locale locale) {
        logger.info("Incoming request to update product document for user: {}", username);

        ProductDocumentResponse response = productDocumentService.update(request, username, locale);

        logger.info("Outgoing response after updating product document: Success: {}", 
                response != null ? response.isSuccess() : false);

        return response;
    }

    @Override
    public ProductDocumentResponse delete(Long id, Locale locale, String username) {
        logger.info("Incoming request to delete product document by ID: {} for user: {}", id, username);

        ProductDocumentResponse response = productDocumentService.delete(id, locale, username);

        logger.info("Outgoing response after deleting product document: Success: {}", 
                response != null ? response.isSuccess() : false);

        return response;
    }

    @Override
    public ProductDocumentResponse findByMultipleFilters(ProductDocumentMultipleFiltersRequest request, String username, Locale locale) {
        logger.info("Incoming request to find product documents by multiple filters for user: {}", username);

        ProductDocumentResponse response = productDocumentService.findByMultipleFilters(request, username, locale);

        logger.info("Outgoing response after finding product documents by filters: Success: {}", 
                response != null ? response.isSuccess() : false);

        return response;
    }

    @Override
    public byte[] exportToCsv(List<ProductDocumentDto> items) {
        logger.info("Incoming request to export {} product documents to CSV", 
                items != null ? items.size() : 0);

        byte[] result = productDocumentService.exportToCsv(items);

        logger.info("Outgoing response after exporting to CSV: Size: {} bytes", 
                result != null ? result.length : 0);

        return result;
    }

    @Override
    public byte[] exportToExcel(List<ProductDocumentDto> items) throws IOException {
        logger.info("Incoming request to export {} product documents to Excel", 
                items != null ? items.size() : 0);

        byte[] result = productDocumentService.exportToExcel(items);

        logger.info("Outgoing response after exporting to Excel: Size: {} bytes", 
                result != null ? result.length : 0);

        return result;
    }

    @Override
    public byte[] exportToPdf(List<ProductDocumentDto> items) throws DocumentException {
        logger.info("Incoming request to export {} product documents to PDF", 
                items != null ? items.size() : 0);

        byte[] result = productDocumentService.exportToPdf(items);

        logger.info("Outgoing response after exporting to PDF: Size: {} bytes", 
                result != null ? result.length : 0);

        return result;
    }

    @Override
    public ImportSummary importProductDocumentFromCsv(InputStream csvInputStream) throws IOException {
        logger.info("Incoming request to import product documents from CSV");

        ImportSummary result = productDocumentService.importProductDocumentFromCsv(csvInputStream);

        logger.info("Outgoing response after importing from CSV: Success: {}", 
                result != null);

        return result;
    }
}
