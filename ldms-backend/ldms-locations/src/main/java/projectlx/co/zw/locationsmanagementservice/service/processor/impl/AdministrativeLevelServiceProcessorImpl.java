package projectlx.co.zw.locationsmanagementservice.service.processor.impl;

import com.lowagie.text.DocumentException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import projectlx.co.zw.locationsmanagementservice.business.logic.api.AdministrativeLevelService;
import projectlx.co.zw.locationsmanagementservice.service.processor.api.AdministrativeLevelServiceProcessor;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.AdministrativeLevelDto;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.ImportSummary;
import projectlx.co.zw.locationsmanagementservice.utils.requests.CreateAdministrativeLevelRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.EditAdministrativeLevelRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.AdministrativeLevelMultipleFiltersRequest;
import projectlx.co.zw.locationsmanagementservice.utils.responses.AdministrativeLevelResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

@RequiredArgsConstructor
public class AdministrativeLevelServiceProcessorImpl implements AdministrativeLevelServiceProcessor {

    private final AdministrativeLevelService administrativeLevelService;
    private final Logger logger = LoggerFactory.getLogger(AdministrativeLevelServiceProcessorImpl.class);

    @Override
    public AdministrativeLevelResponse create(CreateAdministrativeLevelRequest request, Locale locale, String username) {
        logger.info("Incoming request to create an administrative level : {}", request);

        AdministrativeLevelResponse administrativeLevelResponse = administrativeLevelService.create(request, locale, username);

        logger.info("Outgoing response after creating an administrative level : {}. Status Code: {}. Message: {}",
                administrativeLevelResponse, administrativeLevelResponse.getStatusCode(), administrativeLevelResponse.getMessage());

        return administrativeLevelResponse;
    }

    @Override
    public AdministrativeLevelResponse update(EditAdministrativeLevelRequest request, Locale locale, String username) {
        logger.info("Incoming request to update an administrative level : {}", request);

        AdministrativeLevelResponse administrativeLevelResponse = administrativeLevelService.update(request, username, locale);

        logger.info("Outgoing response after updating an administrative level : {}. Status Code: {}. Message: {}",
                administrativeLevelResponse, administrativeLevelResponse.getStatusCode(), administrativeLevelResponse.getMessage());

        return administrativeLevelResponse;
    }

    @Override
    public AdministrativeLevelResponse findById(Long id, Locale locale, String username) {
        logger.info("Incoming request to find an administrative level by id: {}", id);

        AdministrativeLevelResponse administrativeLevelResponse = administrativeLevelService.findById(id, locale, username);

        logger.info("Outgoing response after finding an administrative level by id : {}. Status Code: {}. Message: {}",
                administrativeLevelResponse, administrativeLevelResponse.getStatusCode(), administrativeLevelResponse.getMessage());

        return administrativeLevelResponse;
    }

    @Override
    public AdministrativeLevelResponse findAll(Locale locale, String username) {
        logger.info("Incoming request to find all administrative levels as a list");

        AdministrativeLevelResponse administrativeLevelResponse = administrativeLevelService.findAllAsList(locale, username);

        logger.info("Outgoing response after finding all administrative levels as a list : {}. Status Code: {}. Message: {}",
                administrativeLevelResponse, administrativeLevelResponse.getStatusCode(), administrativeLevelResponse.getMessage());

        return administrativeLevelResponse;
    }

    @Override
    public AdministrativeLevelResponse delete(Long id, Locale locale, String username) {
        logger.info("Incoming request to delete an administrative level with the id : {}", id);

        AdministrativeLevelResponse administrativeLevelResponse = administrativeLevelService.delete(id, locale, username);

        logger.info("Outgoing response after deleting an administrative level: {}. Status Code: {}. Message: {}", administrativeLevelResponse,
                administrativeLevelResponse.getStatusCode(), administrativeLevelResponse.getMessage());

        return administrativeLevelResponse;
    }

    @Override
    public AdministrativeLevelResponse findByMultipleFilters(AdministrativeLevelMultipleFiltersRequest request, String username, Locale locale) {
        logger.info("Incoming request to find an administrative level using multiple filters : {}", request);

        AdministrativeLevelResponse administrativeLevelResponse = administrativeLevelService.findByMultipleFilters(request, username, locale);

        logger.info("Outgoing response after finding an administrative level using multiple filters: {}. Status Code: {}. Message: {}",
                administrativeLevelResponse, administrativeLevelResponse.getStatusCode(), administrativeLevelResponse.getMessage());

        return administrativeLevelResponse;
    }

    @Override
    public byte[] exportToCsv(List<AdministrativeLevelDto> dtoList) {
        logger.info("Incoming request to export administrative levels to CSV. List size: {}", dtoList.size());

        byte[] csvData = administrativeLevelService.exportToCsv(dtoList);

        logger.info("Outgoing CSV export complete. Byte size: {}", csvData.length);

        return csvData;
    }

    @Override
    public byte[] exportToExcel(List<AdministrativeLevelDto> dtoList) throws IOException {
        logger.info("Incoming request to export administrative levels to Excel. List size: {}", dtoList.size());

        byte[] excelData = administrativeLevelService.exportToExcel(dtoList);

        logger.info("Outgoing Excel export complete. Byte size: {}", excelData.length);

        return excelData;
    }

    @Override
    public byte[] exportToPdf(List<AdministrativeLevelDto> dtoList) throws DocumentException {
        logger.info("Incoming request to export administrative levels to PDF. List size: {}", dtoList.size());

        byte[] pdfData = administrativeLevelService.exportToPdf(dtoList);

        logger.info("Outgoing PDF export complete. Byte size: {}", pdfData.length);

        return pdfData;
    }

    @Override
    public ImportSummary importFromCsv(InputStream csvInputStream) throws IOException {
        logger.info("Incoming request to import administrative levels from CSV");

        ImportSummary importSummary = administrativeLevelService.importAdministrativeLevelFromCsv(csvInputStream);

        logger.info("Outgoing response after importing administrative levels from CSV: Total: {}, Success: {}, Failed: {}",
                importSummary.total, importSummary.success, importSummary.failed);

        return importSummary;
    }
}
