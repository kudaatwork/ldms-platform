package projectlx.inventory.management.service.processor.impl;

import com.lowagie.text.DocumentException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import projectlx.inventory.management.business.logic.api.WarehouseLocationService;
import projectlx.inventory.management.service.processor.api.WarehouseLocationServiceProcessor;
import projectlx.inventory.management.utils.dtos.ImportSummary;
import projectlx.inventory.management.utils.dtos.WarehouseLocationDto;
import projectlx.inventory.management.utils.requests.CreateWarehouseLocationRequest;
import projectlx.inventory.management.utils.requests.EditWarehouseLocationRequest;
import projectlx.inventory.management.utils.requests.GrantWarehouseAccessRequest;
import projectlx.inventory.management.utils.requests.WarehouseLocationMultipleFiltersRequest;
import projectlx.inventory.management.utils.responses.WarehouseLocationResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class WarehouseLocationServiceProcessorImpl implements WarehouseLocationServiceProcessor {

    private final WarehouseLocationService warehouseLocationService;
    private static final Logger logger = LoggerFactory.getLogger(WarehouseLocationServiceProcessorImpl.class);

    @Override
    public WarehouseLocationResponse create(CreateWarehouseLocationRequest request, Locale locale, String username) {
        logger.info("Incoming request to create a warehouse location: {}", request);

        WarehouseLocationResponse response = warehouseLocationService.create(request, locale, username);

        logger.info("Outgoing response after creating a warehouse location: {}. Status Code: {}. Message: {}",
                response, response.getStatusCode(), response.getMessage());

        return response;
    }

    @Override
    public WarehouseLocationResponse findById(Long id, Locale locale, String username) {
        logger.info("Incoming request to find a warehouse location by id: {}", id);

        WarehouseLocationResponse response = warehouseLocationService.findById(id, locale, username);

        logger.info("Outgoing response after finding a warehouse location by id: {}. Status Code: {}. Message: {}",
                response, response.getStatusCode(), response.getMessage());

        return response;
    }

    @Override
    public WarehouseLocationResponse findAllAsList(Locale locale, String username) {
        logger.info("Incoming request to find all warehouse locations as a list");

        WarehouseLocationResponse response = warehouseLocationService.findAllAsList(locale, username);

        logger.info("Outgoing response after finding all warehouse locations as a list: {}. Status Code: {}. Message: {}",
                response, response.getStatusCode(), response.getMessage());

        return response;
    }

    @Override
    public WarehouseLocationResponse update(EditWarehouseLocationRequest request, String username, Locale locale) {
        logger.info("Incoming request to update a warehouse location: {}", request);

        WarehouseLocationResponse response = warehouseLocationService.update(request, username, locale);

        logger.info("Outgoing response after updating a warehouse location: {}. Status Code: {}. Message: {}",
                response, response.getStatusCode(), response.getMessage());

        return response;
    }

    @Override
    public WarehouseLocationResponse delete(Long id, Locale locale, String username) {
        logger.info("Incoming request to delete a warehouse location with the id: {}", id);

        WarehouseLocationResponse response = warehouseLocationService.delete(id, locale, username);

        logger.info("Outgoing response after deleting a warehouse location: {}. Status Code: {}. Message: {}",
                response, response.getStatusCode(), response.getMessage());

        return response;
    }

    @Override
    public WarehouseLocationResponse findByMultipleFilters(WarehouseLocationMultipleFiltersRequest request, String username, Locale locale) {
        logger.info("Incoming request to find warehouse locations using multiple filters: {}", request);

        WarehouseLocationResponse response = warehouseLocationService.findByMultipleFilters(request, username, locale);

        logger.info("Outgoing response after finding warehouse locations using multiple filters: {}. Status Code: {}. Message: {}",
                response, response.getStatusCode(), response.getMessage());

        return response;
    }

    @Override
    public WarehouseLocationResponse grantOrganizationAccess(GrantWarehouseAccessRequest request, Locale locale, String username) {
        logger.info("Incoming request to grant warehouse access: {}", request);
        WarehouseLocationResponse response = warehouseLocationService.grantOrganizationAccess(request, locale, username);
        logger.info("Outgoing response after granting warehouse access. Status Code: {}", response.getStatusCode());
        return response;
    }

    @Override
    public WarehouseLocationResponse revokeOrganizationAccess(Long warehouseLocationId, Long grantedOrganizationId,
                                                              Locale locale, String username) {
        logger.info("Incoming request to revoke warehouse access warehouseId={} orgId={}", warehouseLocationId, grantedOrganizationId);
        WarehouseLocationResponse response = warehouseLocationService.revokeOrganizationAccess(
                warehouseLocationId, grantedOrganizationId, locale, username);
        logger.info("Outgoing response after revoking warehouse access. Status Code: {}", response.getStatusCode());
        return response;
    }

    @Override
    public WarehouseLocationResponse listOrganizationAccess(Long warehouseLocationId, Locale locale, String username) {
        logger.info("Incoming request to list warehouse access grants for warehouseId={}", warehouseLocationId);
        WarehouseLocationResponse response = warehouseLocationService.listOrganizationAccess(warehouseLocationId, locale, username);
        logger.info("Outgoing response after listing warehouse access grants. Status Code: {}", response.getStatusCode());
        return response;
    }

    @Override
    public byte[] exportToCsv(List<WarehouseLocationDto> items) {
        logger.info("Incoming request to export warehouse locations to CSV. Item count: {}", items.size());

        byte[] csvData = warehouseLocationService.exportToCsv(items);

        logger.info("Outgoing CSV export complete. Byte size: {}", csvData.length);

        return csvData;
    }

    @Override
    public byte[] exportToExcel(List<WarehouseLocationDto> items) throws IOException {
        logger.info("Incoming request to export warehouse locations to Excel. Item count: {}", items.size());

        byte[] excelData = warehouseLocationService.exportToExcel(items);

        logger.info("Outgoing Excel export complete. Byte size: {}", excelData.length);

        return excelData;
    }

    @Override
    public byte[] exportToPdf(List<WarehouseLocationDto> items) throws DocumentException {
        logger.info("Incoming request to export warehouse locations to PDF. Item count: {}", items.size());

        byte[] pdfData = warehouseLocationService.exportToPdf(items);

        logger.info("Outgoing PDF export complete. Byte size: {}", pdfData.length);

        return pdfData;
    }

    @Override
    public ImportSummary importWarehouseLocationFromCsv(InputStream csvInputStream) throws IOException {
        logger.info("Incoming request to import warehouse locations from CSV");

        ImportSummary importSummary = warehouseLocationService.importWarehouseLocationFromCsv(csvInputStream);

        logger.info("Outgoing response after importing warehouse locations from CSV: Total: {}, Success: {}, Failed: {}",
                importSummary.total, importSummary.success, importSummary.failed);

        return importSummary;
    }
}