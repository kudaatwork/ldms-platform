package projectlx.co.zw.locationsmanagementservice.service.processor.impl;

import com.lowagie.text.DocumentException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import projectlx.co.zw.locationsmanagementservice.business.logic.api.LanguageService;
import projectlx.co.zw.locationsmanagementservice.service.processor.api.LanguageServiceProcessor;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.LanguageDto;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.ImportSummary;
import projectlx.co.zw.locationsmanagementservice.utils.requests.CreateLanguageRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.EditLanguageRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.LanguageMultipleFiltersRequest;
import projectlx.co.zw.locationsmanagementservice.utils.responses.LanguageResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

@RequiredArgsConstructor
public class LanguageServiceProcessorImpl implements LanguageServiceProcessor {

    private final LanguageService languageService;
    private final Logger logger = LoggerFactory.getLogger(LanguageServiceProcessorImpl.class);

    @Override
    public LanguageResponse create(CreateLanguageRequest request, Locale locale, String username) {
        logger.info("Incoming request to create a language : {}", request);

        LanguageResponse languageResponse = languageService.create(request, locale, username);

        logger.info("Outgoing response after creating a language : {}. Status Code: {}. Message: {}",
                languageResponse, languageResponse.getStatusCode(), languageResponse.getMessage());

        return languageResponse;
    }

    @Override
    public LanguageResponse update(EditLanguageRequest request, Locale locale, String username) {
        logger.info("Incoming request to update a language : {}", request);

        LanguageResponse languageResponse = languageService.update(request, username, locale);

        logger.info("Outgoing response after updating a language : {}. Status Code: {}. Message: {}",
                languageResponse, languageResponse.getStatusCode(), languageResponse.getMessage());

        return languageResponse;
    }

    @Override
    public LanguageResponse findById(Long id, Locale locale, String username) {
        logger.info("Incoming request to find a language by id: {}", id);

        LanguageResponse languageResponse = languageService.findById(id, locale, username);

        logger.info("Outgoing response after finding a language by id : {}. Status Code: {}. Message: {}",
                languageResponse, languageResponse.getStatusCode(), languageResponse.getMessage());

        return languageResponse;
    }

    @Override
    public LanguageResponse findAll(Locale locale, String username) {
        logger.info("Incoming request to find all languages as a list");

        LanguageResponse languageResponse = languageService.findAllAsList(locale, username);

        logger.info("Outgoing response after finding all languages as a list : {}. Status Code: {}. Message: {}",
                languageResponse, languageResponse.getStatusCode(), languageResponse.getMessage());

        return languageResponse;
    }

    @Override
    public LanguageResponse delete(Long id, Locale locale, String username) {
        logger.info("Incoming request to delete a language with the id : {}", id);

        LanguageResponse languageResponse = languageService.delete(id, locale, username);

        logger.info("Outgoing response after deleting a language: {}. Status Code: {}. Message: {}", languageResponse,
                languageResponse.getStatusCode(), languageResponse.getMessage());

        return languageResponse;
    }

    @Override
    public LanguageResponse findByMultipleFilters(LanguageMultipleFiltersRequest request, String username, Locale locale) {
        logger.info("Incoming request to find a language using multiple filters : {}", request);

        LanguageResponse languageResponse = languageService.findByMultipleFilters(request, username, locale);

        logger.info("Outgoing response after finding a language using multiple filters: {}. Status Code: {}. Message: {}",
                languageResponse, languageResponse.getStatusCode(), languageResponse.getMessage());

        return languageResponse;
    }

    @Override
    public byte[] exportToCsv(List<LanguageDto> dtoList) {
        logger.info("Incoming request to export languages to CSV. List size: {}", dtoList.size());

        byte[] csvData = languageService.exportToCsv(dtoList);

        logger.info("Outgoing CSV export complete. Byte size: {}", csvData.length);

        return csvData;
    }

    @Override
    public byte[] exportToExcel(List<LanguageDto> dtoList) throws IOException {
        logger.info("Incoming request to export languages to Excel. List size: {}", dtoList.size());

        byte[] excelData = languageService.exportToExcel(dtoList);

        logger.info("Outgoing Excel export complete. Byte size: {}", excelData.length);

        return excelData;
    }

    @Override
    public byte[] exportToPdf(List<LanguageDto> dtoList) throws DocumentException {
        logger.info("Incoming request to export languages to PDF. List size: {}", dtoList.size());

        byte[] pdfData = languageService.exportToPdf(dtoList);

        logger.info("Outgoing PDF export complete. Byte size: {}", pdfData.length);

        return pdfData;
    }

    @Override
    public ImportSummary importFromCsv(InputStream csvInputStream) throws IOException {
        logger.info("Incoming request to import languages from CSV");

        ImportSummary importSummary = languageService.importLanguageFromCsv(csvInputStream);

        logger.info("Outgoing response after importing languages from CSV: Total: {}, Success: {}, Failed: {}",
                importSummary.total, importSummary.success, importSummary.failed);

        return importSummary;
    }
}
