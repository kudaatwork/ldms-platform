package projectlx.co.zw.locationsmanagementservice.service.processor.impl;

import com.lowagie.text.DocumentException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import projectlx.co.zw.locationsmanagementservice.business.logic.api.SuburbService;
import projectlx.co.zw.locationsmanagementservice.service.processor.api.SuburbServiceProcessor;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.SuburbDto;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.ImportSummary;
import projectlx.co.zw.locationsmanagementservice.utils.requests.CreateSuburbRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.EditSuburbRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.SuburbMultipleFiltersRequest;
import projectlx.co.zw.locationsmanagementservice.utils.responses.SuburbResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

@RequiredArgsConstructor
public class SuburbServiceProcessorImpl implements SuburbServiceProcessor {

    private final SuburbService suburbService;
    private final Logger logger = LoggerFactory.getLogger(SuburbServiceProcessorImpl.class);

    @Override
    public SuburbResponse create(CreateSuburbRequest request, Locale locale, String username) {
        logger.info("Incoming request to create a suburb : {}", request);

        SuburbResponse suburbResponse = suburbService.create(request, locale, username);

        logger.info("Outgoing response after creating a suburb : {}. Status Code: {}. Message: {}",
                suburbResponse, suburbResponse.getStatusCode(), suburbResponse.getMessage());

        return suburbResponse;
    }

    @Override
    public SuburbResponse update(EditSuburbRequest request, Locale locale, String username) {
        logger.info("Incoming request to update a suburb : {}", request);

        SuburbResponse suburbResponse = suburbService.update(request, username, locale);

        logger.info("Outgoing response after updating a suburb : {}. Status Code: {}. Message: {}",
                suburbResponse, suburbResponse.getStatusCode(), suburbResponse.getMessage());

        return suburbResponse;
    }

    @Override
    public SuburbResponse findById(Long id, Locale locale, String username) {
        logger.info("Incoming request to find a suburb by id: {}", id);

        SuburbResponse suburbResponse = suburbService.findById(id, locale, username);

        logger.info("Outgoing response after finding a suburb by id : {}. Status Code: {}. Message: {}",
                suburbResponse, suburbResponse.getStatusCode(), suburbResponse.getMessage());

        return suburbResponse;
    }

    @Override
    public SuburbResponse findAll(Locale locale, String username) {
        logger.info("Incoming request to find all suburbs as a list");

        SuburbResponse suburbResponse = suburbService.findAllAsList(locale, username);

        logger.info("Outgoing response after finding all suburbs as a list : {}. Status Code: {}. Message: {}",
                suburbResponse, suburbResponse.getStatusCode(), suburbResponse.getMessage());

        return suburbResponse;
    }

    @Override
    public SuburbResponse delete(Long id, Locale locale, String username) {
        logger.info("Incoming request to delete a suburb with the id : {}", id);

        SuburbResponse suburbResponse = suburbService.delete(id, locale, username);

        logger.info("Outgoing response after deleting a suburb: {}. Status Code: {}. Message: {}", suburbResponse,
                suburbResponse.getStatusCode(), suburbResponse.getMessage());

        return suburbResponse;
    }

    @Override
    public SuburbResponse findByMultipleFilters(SuburbMultipleFiltersRequest request, String username, Locale locale) {
        logger.info("Incoming request to find a suburb using multiple filters : {}", request);

        SuburbResponse suburbResponse = suburbService.findByMultipleFilters(request, username, locale);

        logger.info("Outgoing response after finding a suburb using multiple filters: {}. Status Code: {}. Message: {}",
                suburbResponse, suburbResponse.getStatusCode(), suburbResponse.getMessage());

        return suburbResponse;
    }

    @Override
    public byte[] exportToCsv(List<SuburbDto> dtoList) {
        logger.info("Incoming request to export suburbs to CSV. List size: {}", dtoList.size());

        byte[] csvData = suburbService.exportToCsv(dtoList);

        logger.info("Outgoing CSV export complete. Byte size: {}", csvData.length);

        return csvData;
    }

    @Override
    public byte[] exportToExcel(List<SuburbDto> dtoList) throws IOException {
        logger.info("Incoming request to export suburbs to Excel. List size: {}", dtoList.size());

        byte[] excelData = suburbService.exportToExcel(dtoList);

        logger.info("Outgoing Excel export complete. Byte size: {}", excelData.length);

        return excelData;
    }

    @Override
    public byte[] exportToPdf(List<SuburbDto> dtoList) throws DocumentException {
        logger.info("Incoming request to export suburbs to PDF. List size: {}", dtoList.size());

        byte[] pdfData = suburbService.exportToPdf(dtoList);

        logger.info("Outgoing PDF export complete. Byte size: {}", pdfData.length);

        return pdfData;
    }

    @Override
    public ImportSummary importFromCsv(InputStream csvInputStream) throws IOException {
        logger.info("Incoming request to import suburbs from CSV");

        ImportSummary importSummary = suburbService.importSuburbFromCsv(csvInputStream);

        logger.info("Outgoing response after importing suburbs from CSV: Total: {}, Success: {}, Failed: {}",
                importSummary.total, importSummary.success, importSummary.failed);

        return importSummary;
    }
}
