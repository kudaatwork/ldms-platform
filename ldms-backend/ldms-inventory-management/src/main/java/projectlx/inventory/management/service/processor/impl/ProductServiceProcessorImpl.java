package projectlx.inventory.management.service.processor.impl;

import com.lowagie.text.DocumentException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import projectlx.inventory.management.business.logic.api.ProductService;
import projectlx.inventory.management.service.processor.api.ProductServiceProcessor;
import projectlx.inventory.management.utils.dtos.ProductDto;
import projectlx.inventory.management.utils.dtos.ImportSummary;
import projectlx.inventory.management.utils.requests.CreateProductRequest;
import projectlx.inventory.management.utils.requests.EditProductRequest;
import projectlx.inventory.management.utils.requests.ProductMultipleFiltersRequest;
import projectlx.inventory.management.utils.responses.ProductResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ProductServiceProcessorImpl implements ProductServiceProcessor {

    private final ProductService productService;
    private static final Logger logger = LoggerFactory.getLogger(ProductServiceProcessorImpl.class);

    @Override
    public ProductResponse create(CreateProductRequest request, Locale locale, String username) {
        logger.info("Incoming request to create product for user: {}", username);

        ProductResponse response = productService.create(request, locale, username);

        logger.info("Outgoing response after creating product: Success: {}", 
                response != null ? response.isSuccess() : false);

        return response;
    }

    @Override
    public ProductResponse findById(Long id, Locale locale, String username) {
        logger.info("Incoming request to find product by ID: {} for user: {}", id, username);

        ProductResponse response = productService.findById(id, locale, username);

        logger.info("Outgoing response after finding product by ID: Success: {}", 
                response != null ? response.isSuccess() : false);

        return response;
    }

    @Override
    public ProductResponse findAllAsList(Locale locale, String username) {
        logger.info("Incoming request to find all products as list for user: {}", username);

        ProductResponse response = productService.findAllAsList(locale, username);

        logger.info("Outgoing response after finding all products: Success: {}", 
                response != null ? response.isSuccess() : false);

        return response;
    }

    @Override
    public ProductResponse update(EditProductRequest request, String username, Locale locale) {
        logger.info("Incoming request to update product for user: {}", username);

        ProductResponse response = productService.update(request, username, locale);

        logger.info("Outgoing response after updating product: Success: {}", 
                response != null ? response.isSuccess() : false);

        return response;
    }

    @Override
    public ProductResponse delete(Long id, Locale locale, String username) {
        logger.info("Incoming request to delete product by ID: {} for user: {}", id, username);

        ProductResponse response = productService.delete(id, locale, username);

        logger.info("Outgoing response after deleting product: Success: {}", 
                response != null ? response.isSuccess() : false);

        return response;
    }

    @Override
    public ProductResponse findByMultipleFilters(ProductMultipleFiltersRequest request, String username, Locale locale) {
        logger.info("Incoming request to find products by multiple filters for user: {}", username);

        ProductResponse response = productService.findByMultipleFilters(request, username, locale);

        logger.info("Outgoing response after finding products by filters: Success: {}", 
                response != null ? response.isSuccess() : false);

        return response;
    }

    @Override
    public byte[] exportToCsv(List<ProductDto> items) {
        logger.info("Incoming request to export {} products to CSV", 
                items != null ? items.size() : 0);

        byte[] result = productService.exportToCsv(items);

        logger.info("Outgoing response after exporting to CSV: Size: {} bytes", 
                result != null ? result.length : 0);

        return result;
    }

    @Override
    public byte[] exportToExcel(List<ProductDto> items) throws IOException {
        logger.info("Incoming request to export {} products to Excel", 
                items != null ? items.size() : 0);

        byte[] result = productService.exportToExcel(items);

        logger.info("Outgoing response after exporting to Excel: Size: {} bytes", 
                result != null ? result.length : 0);

        return result;
    }

    @Override
    public byte[] exportToPdf(List<ProductDto> items) throws DocumentException {
        logger.info("Incoming request to export {} products to PDF", 
                items != null ? items.size() : 0);

        byte[] result = productService.exportToPdf(items);

        logger.info("Outgoing response after exporting to PDF: Size: {} bytes", 
                result != null ? result.length : 0);

        return result;
    }

    @Override
    public ImportSummary importProductFromCsv(InputStream csvInputStream) throws IOException {
        logger.info("Incoming request to import products from CSV");

        ImportSummary result = productService.importProductFromCsv(csvInputStream);

        logger.info("Outgoing response after importing from CSV: Success: {}", 
                result != null);

        return result;
    }
}
