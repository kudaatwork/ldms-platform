package projectlx.inventory.management.business.logic.api;

import com.lowagie.text.DocumentException;
import projectlx.inventory.management.utils.dtos.ProductSubCategoryDto;
import projectlx.inventory.management.utils.dtos.ImportSummary;
import projectlx.inventory.management.utils.requests.ProductSubCategoryMultipleFiltersRequest;
import projectlx.inventory.management.utils.requests.CreateProductSubCategoryRequest;
import projectlx.inventory.management.utils.requests.EditProductSubCategoryRequest;
import projectlx.inventory.management.utils.responses.ProductSubCategoryResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

public interface ProductSubCategoryService {
    ProductSubCategoryResponse create(CreateProductSubCategoryRequest request, Locale locale, String username);
    ProductSubCategoryResponse findById(Long id, Locale locale, String username);
    ProductSubCategoryResponse findAllAsList(Locale locale, String username);
    ProductSubCategoryResponse update(EditProductSubCategoryRequest request, String username, Locale locale);
    ProductSubCategoryResponse delete(Long id, Locale locale, String username);
    ProductSubCategoryResponse findByMultipleFilters(ProductSubCategoryMultipleFiltersRequest request, String username, Locale locale);
    byte[] exportToCsv(List<ProductSubCategoryDto> items);
    byte[] exportToExcel(List<ProductSubCategoryDto> items) throws IOException;
    byte[] exportToPdf(List<ProductSubCategoryDto> items) throws DocumentException;
    ImportSummary importProductSubCategoryFromCsv(InputStream csvInputStream) throws IOException;
}
