package projectlx.inventory.management.business.logic.api;

import com.lowagie.text.DocumentException;
import projectlx.inventory.management.utils.dtos.ProductCategoryDto;
import projectlx.inventory.management.utils.dtos.ImportSummary;
import projectlx.inventory.management.utils.requests.ProductCategoryMultipleFiltersRequest;
import projectlx.inventory.management.utils.requests.CreateProductCategoryRequest;
import projectlx.inventory.management.utils.requests.EditProductCategoryRequest;
import projectlx.inventory.management.utils.responses.ProductCategoryResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

public interface ProductCategoryService {
    ProductCategoryResponse create(CreateProductCategoryRequest request, Locale locale, String username);
    ProductCategoryResponse findById(Long id, Locale locale, String username);
    ProductCategoryResponse findAllAsList(Locale locale, String username);
    ProductCategoryResponse update(EditProductCategoryRequest request, String username, Locale locale);
    ProductCategoryResponse delete(Long id, Locale locale, String username);
    ProductCategoryResponse findByMultipleFilters(ProductCategoryMultipleFiltersRequest request, String username, Locale locale);
    byte[] exportToCsv(List<ProductCategoryDto> items);
    byte[] exportToExcel(List<ProductCategoryDto> items) throws IOException;
    byte[] exportToPdf(List<ProductCategoryDto> items) throws DocumentException;
    ImportSummary importProductCategoryFromCsv(InputStream csvInputStream) throws IOException;
}
