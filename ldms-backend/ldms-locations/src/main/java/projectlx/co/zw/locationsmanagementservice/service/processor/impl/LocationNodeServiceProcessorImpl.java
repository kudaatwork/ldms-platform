package projectlx.co.zw.locationsmanagementservice.service.processor.impl;

import com.lowagie.text.DocumentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import projectlx.co.zw.locationsmanagementservice.business.logic.api.LocationNodeService;
import projectlx.co.zw.locationsmanagementservice.service.processor.api.LocationNodeServiceProcessor;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.ImportSummary;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.LocationNodeDto;
import projectlx.co.zw.locationsmanagementservice.utils.requests.CreateLocationNodeRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.EditLocationNodeRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.LocationNodeMultipleFiltersRequest;
import projectlx.co.zw.locationsmanagementservice.utils.responses.LocationNodeResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

@RequiredArgsConstructor
@Slf4j
public class LocationNodeServiceProcessorImpl implements LocationNodeServiceProcessor {
    private final LocationNodeService locationNodeService;

    @Override
    public LocationNodeResponse create(CreateLocationNodeRequest request, Locale locale, String username) {
        log.info("Incoming create location-node request: {}", request);
        return locationNodeService.create(request, locale, username);
    }

    @Override
    public LocationNodeResponse update(EditLocationNodeRequest request, Locale locale, String username) {
        log.info("Incoming update location-node request: {}", request);
        return locationNodeService.update(request, locale, username);
    }

    @Override
    public LocationNodeResponse findById(Long id, Locale locale, String username) {
        return locationNodeService.findById(id, locale, username);
    }

    @Override
    public LocationNodeResponse findByParentId(Long parentId, Locale locale, String username) {
        return locationNodeService.findByParentId(parentId, locale, username);
    }

    @Override
    public LocationNodeResponse findByMultipleFilters(LocationNodeMultipleFiltersRequest request, Locale locale, String username) {
        return locationNodeService.findByMultipleFilters(request, locale, username);
    }

    @Override
    public LocationNodeResponse findAllAsList(Locale locale, String username) {
        return locationNodeService.findAllAsList(locale, username);
    }

    @Override
    public LocationNodeResponse delete(Long id, Locale locale, String username) {
        return locationNodeService.delete(id, locale, username);
    }

    @Override
    public byte[] exportToCsv(List<LocationNodeDto> dtoList) {
        log.info("Incoming request to export location nodes to CSV. List size: {}", dtoList.size());
        byte[] csvData = locationNodeService.exportToCsv(dtoList);
        log.info("Outgoing CSV export complete. Byte size: {}", csvData.length);
        return csvData;
    }

    @Override
    public byte[] exportToExcel(List<LocationNodeDto> dtoList) throws IOException {
        log.info("Incoming request to export location nodes to Excel. List size: {}", dtoList.size());
        byte[] excelData = locationNodeService.exportToExcel(dtoList);
        log.info("Outgoing Excel export complete. Byte size: {}", excelData.length);
        return excelData;
    }

    @Override
    public byte[] exportToPdf(List<LocationNodeDto> dtoList) throws DocumentException {
        log.info("Incoming request to export location nodes to PDF. List size: {}", dtoList.size());
        byte[] pdfData = locationNodeService.exportToPdf(dtoList);
        log.info("Outgoing PDF export complete. Byte size: {}", pdfData.length);
        return pdfData;
    }

    @Override
    public ImportSummary importFromCsv(InputStream csvInputStream) throws IOException {
        log.info("Incoming request to import location nodes from CSV");
        ImportSummary importSummary = locationNodeService.importLocationNodeFromCsv(csvInputStream);
        log.info("Outgoing response after importing location nodes from CSV: Total: {}, Success: {}, Failed: {}",
                importSummary.total, importSummary.importedCount, importSummary.failed);
        return importSummary;
    }
}
