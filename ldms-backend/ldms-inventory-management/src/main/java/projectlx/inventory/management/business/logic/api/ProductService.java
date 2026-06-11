package projectlx.inventory.management.business.logic.api;

import com.lowagie.text.DocumentException;
import projectlx.inventory.management.utils.dtos.ProductDto;
import projectlx.inventory.management.utils.dtos.ImportSummary;
import projectlx.inventory.management.utils.requests.ProductMultipleFiltersRequest;
import projectlx.inventory.management.utils.requests.CreateProductRequest;
import projectlx.inventory.management.utils.requests.EditProductRequest;
import projectlx.inventory.management.utils.responses.ProductResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

public interface ProductService {
    ProductResponse create(CreateProductRequest request, Locale locale, String username);
    ProductResponse findById(Long id, Locale locale, String username);
    ProductResponse findAllAsList(Locale locale, String username);
    ProductResponse update(EditProductRequest request, String username, Locale locale);
    ProductResponse delete(Long id, Locale locale, String username);
    ProductResponse findByMultipleFilters(ProductMultipleFiltersRequest request, String username, Locale locale);
    byte[] exportToCsv(List<ProductDto> items);
    byte[] exportToExcel(List<ProductDto> items) throws IOException;
    byte[] exportToPdf(List<ProductDto> items) throws DocumentException;
    ImportSummary importProductFromCsv(InputStream csvInputStream) throws IOException;
}
