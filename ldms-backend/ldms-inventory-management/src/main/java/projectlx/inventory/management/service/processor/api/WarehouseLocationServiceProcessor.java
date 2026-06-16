package projectlx.inventory.management.service.processor.api;

import com.lowagie.text.DocumentException;
import projectlx.inventory.management.utils.dtos.WarehouseLocationDto;
import projectlx.inventory.management.utils.dtos.ImportSummary;
import projectlx.inventory.management.utils.requests.WarehouseLocationMultipleFiltersRequest;
import projectlx.inventory.management.utils.requests.CreateWarehouseLocationRequest;
import projectlx.inventory.management.utils.requests.EditWarehouseLocationRequest;
import projectlx.inventory.management.utils.requests.GrantWarehouseAccessRequest;
import projectlx.inventory.management.utils.responses.WarehouseLocationResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

public interface WarehouseLocationServiceProcessor {
    WarehouseLocationResponse create(CreateWarehouseLocationRequest request, Locale locale, String username);
    WarehouseLocationResponse findById(Long id, Locale locale, String username);
    WarehouseLocationResponse findAllAsList(Locale locale, String username);
    WarehouseLocationResponse update(EditWarehouseLocationRequest request, String username, Locale locale);
    WarehouseLocationResponse delete(Long id, Locale locale, String username);
    WarehouseLocationResponse findByMultipleFilters(WarehouseLocationMultipleFiltersRequest request, String username, Locale locale);
    WarehouseLocationResponse grantOrganizationAccess(GrantWarehouseAccessRequest request, Locale locale, String username);
    WarehouseLocationResponse revokeOrganizationAccess(Long warehouseLocationId, Long grantedOrganizationId, Locale locale, String username);
    WarehouseLocationResponse listOrganizationAccess(Long warehouseLocationId, Locale locale, String username);
    byte[] exportToCsv(List<WarehouseLocationDto> items);
    byte[] exportToExcel(List<WarehouseLocationDto> items) throws IOException;
    byte[] exportToPdf(List<WarehouseLocationDto> items) throws DocumentException;
    ImportSummary importWarehouseLocationFromCsv(InputStream csvInputStream) throws IOException;
}