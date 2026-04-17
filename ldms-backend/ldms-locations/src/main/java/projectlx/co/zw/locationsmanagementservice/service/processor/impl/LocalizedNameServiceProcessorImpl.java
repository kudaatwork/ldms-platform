package projectlx.co.zw.locationsmanagementservice.service.processor.impl;

import com.lowagie.text.DocumentException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import projectlx.co.zw.locationsmanagementservice.business.logic.api.LocalizedNameService;
import projectlx.co.zw.locationsmanagementservice.service.processor.api.LocalizedNameServiceProcessor;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.LocalizedNameDto;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.ImportSummary;
import projectlx.co.zw.locationsmanagementservice.utils.requests.CreateLocalizedNameRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.EditLocalizedNameRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.LocalizedNameMultipleFiltersRequest;
import projectlx.co.zw.locationsmanagementservice.utils.responses.LocalizedNameResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

@RequiredArgsConstructor
public class LocalizedNameServiceProcessorImpl implements LocalizedNameServiceProcessor {

    private final LocalizedNameService localizedNameService;
    private final Logger logger = LoggerFactory.getLogger(LocalizedNameServiceProcessorImpl.class);

    @Override
    public LocalizedNameResponse create(CreateLocalizedNameRequest request, Locale locale, String username) {
        logger.info("Incoming request to create a localized name : {}", request);

        LocalizedNameResponse localizedNameResponse = localizedNameService.create(request, locale, username);

        logger.info("Outgoing response after creating a localized name : {}. Status Code: {}. Message: {}",
                localizedNameResponse, localizedNameResponse.getStatusCode(), localizedNameResponse.getMessage());

        return localizedNameResponse;
    }

    @Override
    public LocalizedNameResponse update(EditLocalizedNameRequest request, Locale locale, String username) {
        logger.info("Incoming request to update a localized name : {}", request);

        LocalizedNameResponse localizedNameResponse = localizedNameService.update(request, username, locale);

        logger.info("Outgoing response after updating a localized name : {}. Status Code: {}. Message: {}",
                localizedNameResponse, localizedNameResponse.getStatusCode(), localizedNameResponse.getMessage());

        return localizedNameResponse;
    }

    @Override
    public LocalizedNameResponse findById(Long id, Locale locale, String username) {
        logger.info("Incoming request to find a localized name by id: {}", id);

        LocalizedNameResponse localizedNameResponse = localizedNameService.findById(id, locale, username);

        logger.info("Outgoing response after finding a localized name by id : {}. Status Code: {}. Message: {}",
                localizedNameResponse, localizedNameResponse.getStatusCode(), localizedNameResponse.getMessage());

        return localizedNameResponse;
    }

    @Override
    public LocalizedNameResponse findAll(Locale locale, String username) {
        logger.info("Incoming request to find all localized names as a list");

        LocalizedNameResponse localizedNameResponse = localizedNameService.findAllAsList(locale, username);

        logger.info("Outgoing response after finding all localized names as a list : {}. Status Code: {}. Message: {}",
                localizedNameResponse, localizedNameResponse.getStatusCode(), localizedNameResponse.getMessage());

        return localizedNameResponse;
    }

    @Override
    public LocalizedNameResponse delete(Long id, Locale locale, String username) {
        logger.info("Incoming request to delete a localized name with the id : {}", id);

        LocalizedNameResponse localizedNameResponse = localizedNameService.delete(id, locale, username);

        logger.info("Outgoing response after deleting a localized name: {}. Status Code: {}. Message: {}", 
                localizedNameResponse, localizedNameResponse.getStatusCode(), localizedNameResponse.getMessage());

        return localizedNameResponse;
    }

    @Override
    public LocalizedNameResponse findByMultipleFilters(LocalizedNameMultipleFiltersRequest request, String username, Locale locale) {
        logger.info("Incoming request to find a localized name using multiple filters : {}", request);

        LocalizedNameResponse localizedNameResponse = localizedNameService.findByMultipleFilters(request, username, locale);

        logger.info("Outgoing response after finding a localized name using multiple filters: {}. Status Code: {}. Message: {}",
                localizedNameResponse, localizedNameResponse.getStatusCode(), localizedNameResponse.getMessage());

        return localizedNameResponse;
    }

    @Override
    public byte[] exportToCsv(List<LocalizedNameDto> dtoList) {
        logger.info("Incoming request to export localized names to CSV. List size: {}", dtoList.size());

        byte[] csvData = localizedNameService.exportToCsv(dtoList);

        logger.info("Outgoing CSV export complete. Byte size: {}", csvData.length);

        return csvData;
    }

    @Override
    public byte[] exportToExcel(List<LocalizedNameDto> dtoList) throws IOException {
        logger.info("Incoming request to export localized names to Excel. List size: {}", dtoList.size());

        byte[] excelData = localizedNameService.exportToExcel(dtoList);

        logger.info("Outgoing Excel export complete. Byte size: {}", excelData.length);

        return excelData;
    }

    @Override
    public byte[] exportToPdf(List<LocalizedNameDto> dtoList) throws DocumentException {
        logger.info("Incoming request to export localized names to PDF. List size: {}", dtoList.size());

        byte[] pdfData = localizedNameService.exportToPdf(dtoList);

        logger.info("Outgoing PDF export complete. Byte size: {}", pdfData.length);

        return pdfData;
    }

    @Override
    public ImportSummary importFromCsv(InputStream csvInputStream) throws IOException {
        logger.info("Incoming request to import localized names from CSV");

        ImportSummary importSummary = localizedNameService.importLocalizedNameFromCsv(csvInputStream);

        logger.info("Outgoing response after importing localized names from CSV: Total: {}, Success: {}, Failed: {}",
                importSummary.total, importSummary.success, importSummary.failed);

        return importSummary;
    }
}