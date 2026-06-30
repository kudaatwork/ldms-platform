package projectlx.inventory.management.service.processor.api;

import com.lowagie.text.DocumentException;
import projectlx.inventory.management.utils.dtos.DepartmentDto;
import projectlx.inventory.management.utils.dtos.ImportSummary;
import projectlx.inventory.management.utils.requests.CreateDepartmentRequest;
import projectlx.inventory.management.utils.requests.DepartmentMultipleFiltersRequest;
import projectlx.inventory.management.utils.requests.EditDepartmentRequest;
import projectlx.inventory.management.utils.responses.DepartmentResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

public interface DepartmentServiceProcessor {

    DepartmentResponse create(CreateDepartmentRequest request, Locale locale, String username);

    DepartmentResponse findById(Long id, Locale locale, String username);

    DepartmentResponse findAllAsList(Locale locale, String username);

    DepartmentResponse findByMultipleFilters(DepartmentMultipleFiltersRequest request, String username, Locale locale);

    DepartmentResponse update(EditDepartmentRequest request, String username, Locale locale);

    DepartmentResponse delete(Long id, Locale locale, String username);

    byte[] exportToCsv(List<DepartmentDto> items);

    byte[] exportToExcel(List<DepartmentDto> items) throws IOException;

    byte[] exportToPdf(List<DepartmentDto> items) throws DocumentException;

    ImportSummary importDepartmentFromCsv(InputStream csvInputStream, String username, Locale locale) throws IOException;
}
