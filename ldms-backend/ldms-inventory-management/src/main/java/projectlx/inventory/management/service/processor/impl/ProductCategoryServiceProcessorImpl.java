package projectlx.inventory.management.service.processor.impl;

import com.lowagie.text.DocumentException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import projectlx.inventory.management.business.logic.api.ProductCategoryService;
import projectlx.inventory.management.service.processor.api.ProductCategoryServiceProcessor;
import projectlx.inventory.management.utils.dtos.ProductCategoryDto;
import projectlx.inventory.management.utils.dtos.ImportSummary;
import projectlx.inventory.management.utils.requests.CreateProductCategoryRequest;
import projectlx.inventory.management.utils.requests.EditProductCategoryRequest;
import projectlx.inventory.management.utils.requests.ProductCategoryMultipleFiltersRequest;
import projectlx.inventory.management.utils.responses.ProductCategoryResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ProductCategoryServiceProcessorImpl implements ProductCategoryServiceProcessor {

    private final ProductCategoryService productCategoryService;
    private static final Logger logger = LoggerFactory.getLogger(ProductCategoryServiceProcessorImpl.class);

    @Override
    public ProductCategoryResponse create(CreateProductCategoryRequest request, Locale locale, String username) {
        logger.info("Incoming request to create product category for user: {}", username);

        ProductCategoryResponse response = productCategoryService.create(request, locale, username);

        logger.info("Outgoing response after creating product category: Success: {}", 
                response != null ? response.isSuccess() : false);

        return response;
    }

    @Override
    public ProductCategoryResponse findById(Long id, Locale locale, String username) {
        logger.info("Incoming request to find product category by ID: {} for user: {}", id, username);

        ProductCategoryResponse response = productCategoryService.findById(id, locale, username);

        logger.info("Outgoing response after finding product category by ID: Success: {}", 
                response != null ? response.isSuccess() : false);

        return response;
    }

    @Override
    public ProductCategoryResponse findAllAsList(Locale locale, String username) {
        logger.info("Incoming request to find all product categories as list for user: {}", username);

        ProductCategoryResponse response = productCategoryService.findAllAsList(locale, username);

        logger.info("Outgoing response after finding all product categories: Success: {}", 
                response != null ? response.isSuccess() : false);

        return response;
    }

    @Override
    public ProductCategoryResponse update(EditProductCategoryRequest request, String username, Locale locale) {
        logger.info("Incoming request to update product category for user: {}", username);

        ProductCategoryResponse response = productCategoryService.update(request, username, locale);

        logger.info("Outgoing response after updating product category: Success: {}", 
                response != null ? response.isSuccess() : false);

        return response;
    }

    @Override
    public ProductCategoryResponse delete(Long id, Locale locale, String username) {
        logger.info("Incoming request to delete product category by ID: {} for user: {}", id, username);

        ProductCategoryResponse response = productCategoryService.delete(id, locale, username);

        logger.info("Outgoing response after deleting product category: Success: {}", 
                response != null ? response.isSuccess() : false);

        return response;
    }

    @Override
    public ProductCategoryResponse findByMultipleFilters(ProductCategoryMultipleFiltersRequest request, String username, Locale locale) {
        logger.info("Incoming request to find product categories by multiple filters for user: {}", username);

        ProductCategoryResponse response = productCategoryService.findByMultipleFilters(request, username, locale);

        logger.info("Outgoing response after finding product categories by filters: Success: {}", 
                response != null ? response.isSuccess() : false);

        return response;
    }

    @Override
    public byte[] exportToCsv(List<ProductCategoryDto> items) {
        logger.info("Incoming request to export {} product categories to CSV", 
                items != null ? items.size() : 0);

        byte[] result = productCategoryService.exportToCsv(items);

        logger.info("Outgoing response after exporting to CSV: Size: {} bytes", 
                result != null ? result.length : 0);

        return result;
    }

    @Override
    public byte[] exportToExcel(List<ProductCategoryDto> items) throws IOException {
        logger.info("Incoming request to export {} product categories to Excel", 
                items != null ? items.size() : 0);

        byte[] result = productCategoryService.exportToExcel(items);

        logger.info("Outgoing response after exporting to Excel: Size: {} bytes", 
                result != null ? result.length : 0);

        return result;
    }

    @Override
    public byte[] exportToPdf(List<ProductCategoryDto> items) throws DocumentException {
        logger.info("Incoming request to export {} product categories to PDF", 
                items != null ? items.size() : 0);

        byte[] result = productCategoryService.exportToPdf(items);

        logger.info("Outgoing response after exporting to PDF: Size: {} bytes", 
                result != null ? result.length : 0);

        return result;
    }

    @Override
    public ImportSummary importProductCategoryFromCsv(InputStream csvInputStream) throws IOException {
        logger.info("Incoming request to import product categories from CSV");

        ImportSummary result = productCategoryService.importProductCategoryFromCsv(csvInputStream);

        logger.info("Outgoing response after importing from CSV: Success: {}", 
                result != null);

        return result;
    }
}
