package projectlx.inventory.management.service.processor.impl;

import com.lowagie.text.DocumentException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import projectlx.inventory.management.business.logic.api.ProductSubCategoryService;
import projectlx.inventory.management.service.processor.api.ProductSubCategoryServiceProcessor;
import projectlx.inventory.management.utils.dtos.ProductSubCategoryDto;
import projectlx.inventory.management.utils.dtos.ImportSummary;
import projectlx.inventory.management.utils.requests.CreateProductSubCategoryRequest;
import projectlx.inventory.management.utils.requests.EditProductSubCategoryRequest;
import projectlx.inventory.management.utils.requests.ProductSubCategoryMultipleFiltersRequest;
import projectlx.inventory.management.utils.responses.ProductSubCategoryResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ProductSubCategoryServiceProcessorImpl implements ProductSubCategoryServiceProcessor {

    private final ProductSubCategoryService productSubCategoryService;
    private static final Logger logger = LoggerFactory.getLogger(ProductSubCategoryServiceProcessorImpl.class);

    @Override
    public ProductSubCategoryResponse create(CreateProductSubCategoryRequest request, Locale locale, String username) {
        logger.info("Incoming request to create product sub category for user: {}", username);

        ProductSubCategoryResponse response = productSubCategoryService.create(request, locale, username);

        logger.info("Outgoing response after creating product sub category: Success: {}", 
                response != null ? response.isSuccess() : false);

        return response;
    }

    @Override
    public ProductSubCategoryResponse findById(Long id, Locale locale, String username) {
        logger.info("Incoming request to find product sub category by ID: {} for user: {}", id, username);

        ProductSubCategoryResponse response = productSubCategoryService.findById(id, locale, username);

        logger.info("Outgoing response after finding product sub category by ID: Success: {}", 
                response != null ? response.isSuccess() : false);

        return response;
    }

    @Override
    public ProductSubCategoryResponse findAllAsList(Locale locale, String username) {
        logger.info("Incoming request to find all product sub categories as list for user: {}", username);

        ProductSubCategoryResponse response = productSubCategoryService.findAllAsList(locale, username);

        logger.info("Outgoing response after finding all product sub categories: Success: {}", 
                response != null ? response.isSuccess() : false);

        return response;
    }

    @Override
    public ProductSubCategoryResponse update(EditProductSubCategoryRequest request, String username, Locale locale) {
        logger.info("Incoming request to update product sub category for user: {}", username);

        ProductSubCategoryResponse response = productSubCategoryService.update(request, username, locale);

        logger.info("Outgoing response after updating product sub category: Success: {}", 
                response != null ? response.isSuccess() : false);

        return response;
    }

    @Override
    public ProductSubCategoryResponse delete(Long id, Locale locale, String username) {
        logger.info("Incoming request to delete product sub category by ID: {} for user: {}", id, username);

        ProductSubCategoryResponse response = productSubCategoryService.delete(id, locale, username);

        logger.info("Outgoing response after deleting product sub category: Success: {}", 
                response != null ? response.isSuccess() : false);

        return response;
    }

    @Override
    public ProductSubCategoryResponse findByMultipleFilters(ProductSubCategoryMultipleFiltersRequest request, String username, Locale locale) {
        logger.info("Incoming request to find product sub categories by multiple filters for user: {}", username);

        ProductSubCategoryResponse response = productSubCategoryService.findByMultipleFilters(request, username, locale);

        logger.info("Outgoing response after finding product sub categories by filters: Success: {}", 
                response != null ? response.isSuccess() : false);

        return response;
    }

    @Override
    public byte[] exportToCsv(List<ProductSubCategoryDto> items) {
        logger.info("Incoming request to export {} product sub categories to CSV", 
                items != null ? items.size() : 0);

        byte[] result = productSubCategoryService.exportToCsv(items);

        logger.info("Outgoing response after exporting to CSV: Size: {} bytes", 
                result != null ? result.length : 0);

        return result;
    }

    @Override
    public byte[] exportToExcel(List<ProductSubCategoryDto> items) throws IOException {
        logger.info("Incoming request to export {} product sub categories to Excel", 
                items != null ? items.size() : 0);

        byte[] result = productSubCategoryService.exportToExcel(items);

        logger.info("Outgoing response after exporting to Excel: Size: {} bytes", 
                result != null ? result.length : 0);

        return result;
    }

    @Override
    public byte[] exportToPdf(List<ProductSubCategoryDto> items) throws DocumentException {
        logger.info("Incoming request to export {} product sub categories to PDF", 
                items != null ? items.size() : 0);

        byte[] result = productSubCategoryService.exportToPdf(items);

        logger.info("Outgoing response after exporting to PDF: Size: {} bytes", 
                result != null ? result.length : 0);

        return result;
    }

    @Override
    public ImportSummary importProductSubCategoryFromCsv(InputStream csvInputStream) throws IOException {
        logger.info("Incoming request to import product sub categories from CSV");

        ImportSummary result = productSubCategoryService.importProductSubCategoryFromCsv(csvInputStream);

        logger.info("Outgoing response after importing from CSV: Success: {}", 
                result != null);

        return result;
    }
}
