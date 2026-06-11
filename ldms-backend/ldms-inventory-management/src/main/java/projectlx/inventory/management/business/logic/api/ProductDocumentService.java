package projectlx.inventory.management.business.logic.api;

import com.lowagie.text.DocumentException;
import projectlx.inventory.management.utils.dtos.ProductDocumentDto;
import projectlx.inventory.management.utils.dtos.ImportSummary;
import projectlx.inventory.management.utils.requests.ProductDocumentMultipleFiltersRequest;
import projectlx.inventory.management.utils.requests.CreateProductDocumentRequest;
import projectlx.inventory.management.utils.requests.EditProductDocumentRequest;
import projectlx.inventory.management.utils.responses.ProductDocumentResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

public interface ProductDocumentService {
    ProductDocumentResponse create(CreateProductDocumentRequest request, Locale locale, String username);
    ProductDocumentResponse findById(Long id, Locale locale, String username);
    ProductDocumentResponse findAllAsList(Locale locale, String username);
    ProductDocumentResponse update(EditProductDocumentRequest request, String username, Locale locale);
    ProductDocumentResponse delete(Long id, Locale locale, String username);
    ProductDocumentResponse findByMultipleFilters(ProductDocumentMultipleFiltersRequest request, String username, Locale locale);
    byte[] exportToCsv(List<ProductDocumentDto> items);
    byte[] exportToExcel(List<ProductDocumentDto> items) throws IOException;
    byte[] exportToPdf(List<ProductDocumentDto> items) throws DocumentException;
    ImportSummary importProductDocumentFromCsv(InputStream csvInputStream) throws IOException;
}
