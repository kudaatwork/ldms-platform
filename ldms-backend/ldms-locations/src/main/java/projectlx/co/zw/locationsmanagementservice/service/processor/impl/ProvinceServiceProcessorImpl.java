package projectlx.co.zw.locationsmanagementservice.service.processor.impl;

import com.lowagie.text.DocumentException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import projectlx.co.zw.locationsmanagementservice.business.logic.api.ProvinceService;
import projectlx.co.zw.locationsmanagementservice.service.processor.api.ProvinceServiceProcessor;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.ImportSummary;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.ProvinceDto;
import projectlx.co.zw.locationsmanagementservice.utils.requests.CreateProvinceRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.EditProvinceRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.ProvinceMultipleFiltersRequest;
import projectlx.co.zw.locationsmanagementservice.utils.responses.ProvinceResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@RequiredArgsConstructor
public class ProvinceServiceProcessorImpl implements ProvinceServiceProcessor {

    private final ProvinceService provinceService;
    private static final Logger logger = LoggerFactory.getLogger(ProvinceServiceProcessorImpl.class);

    @Override
    public ProvinceResponse create(CreateProvinceRequest request, Locale locale, String username) {
        logger.info("Incoming request to create a province : {}", request);
        ProvinceResponse provinceResponse = provinceService.create(request, locale, username);
        logger.info("Outgoing response after creating a province. Status Code: {}. Message: {}",
                provinceResponse.getStatusCode(), provinceResponse.getMessage());
        return provinceResponse;
    }

    @Override
    public ProvinceResponse update(EditProvinceRequest request, Locale locale, String username) {
        logger.info("Incoming request to update a province : {}", request);
        ProvinceResponse provinceResponse = provinceService.update(request, username, locale);
        logger.info("Outgoing response after updating a province. Status Code: {}. Message: {}",
                provinceResponse.getStatusCode(), provinceResponse.getMessage());
        return provinceResponse;
    }

    @Override
    public ProvinceResponse findById(Long id, Locale locale, String username) {
        logger.info("Incoming request to find a province by id: {}", id);
        ProvinceResponse provinceResponse = provinceService.findById(id, locale, username);
        logger.info("Outgoing response after finding a province by id. Status Code: {}. Message: {}",
                provinceResponse.getStatusCode(), provinceResponse.getMessage());
        return provinceResponse;
    }

    @Override
    public ProvinceResponse findAll(Locale locale, String username) {
        logger.info("Incoming request to find all provinces as a list");
        ProvinceResponse provinceResponse = provinceService.findAllAsList(locale, username);
        logger.info("Outgoing response after finding all provinces as a list. Status Code: {}. Message: {}",
                provinceResponse.getStatusCode(), provinceResponse.getMessage());
        return provinceResponse;
    }

    @Override
    public ProvinceResponse delete(Long id, Locale locale, String username) {
        logger.info("Incoming request to delete a province with the id : {}", id);
        ProvinceResponse provinceResponse = provinceService.delete(id, locale, username);
        logger.info("Outgoing response after deleting a province. Status Code: {}. Message: {}", provinceResponse.getStatusCode(),
                provinceResponse.getMessage());
        return provinceResponse;
    }

    @Override
    public ProvinceResponse findByMultipleFilters(ProvinceMultipleFiltersRequest request, String username, Locale locale) {
        logger.info("Incoming request to find a province using multiple filters : {}", request);
        ProvinceResponse provinceResponse = provinceService.findByMultipleFilters(request, username, locale);
        logger.info("Outgoing response after finding a province using multiple filters. Status Code: {}. Message: {}",
                provinceResponse.getStatusCode(), provinceResponse.getMessage());
        return provinceResponse;
    }

    @Override
    public byte[] exportToCsv(ProvinceMultipleFiltersRequest request, Locale locale, String username) {
        logger.info("Incoming request to export provinces to CSV with filters: {}", request);
        List<ProvinceDto> dtoList = fetchAllMatchingProvinces(request, locale, username);
        byte[] csvData = provinceService.exportToCsv(dtoList);
        logger.info("Outgoing CSV export complete. Exported {} records. Byte size: {}", dtoList.size(), csvData.length);
        return csvData;
    }

    @Override
    public byte[] exportToExcel(ProvinceMultipleFiltersRequest request, Locale locale, String username) throws IOException {
        logger.info("Incoming request to export provinces to Excel with filters: {}", request);
        List<ProvinceDto> dtoList = fetchAllMatchingProvinces(request, locale, username);
        byte[] excelData = provinceService.exportToExcel(dtoList);
        logger.info("Outgoing Excel export complete. Exported {} records. Byte size: {}", dtoList.size(), excelData.length);
        return excelData;
    }

    @Override
    public byte[] exportToPdf(ProvinceMultipleFiltersRequest request, Locale locale, String username) throws DocumentException {
        logger.info("Incoming request to export provinces to PDF with filters: {}", request);
        List<ProvinceDto> dtoList = fetchAllMatchingProvinces(request, locale, username);
        byte[] pdfData = provinceService.exportToPdf(dtoList);
        logger.info("Outgoing PDF export complete. Exported {} records. Byte size: {}", dtoList.size(), pdfData.length);
        return pdfData;
    }

    private List<ProvinceDto> fetchAllMatchingProvinces(ProvinceMultipleFiltersRequest request, Locale locale, String username) {
        // To export all records, we override pagination by setting a very large page size.
        request.setPage(0);
        request.setSize(Integer.MAX_VALUE); // A sane upper limit like 10,000 could also be used.

        ProvinceResponse response = provinceService.findByMultipleFilters(request, username, locale);

        if (response != null && response.isSuccess() && response.getProvinceDtoPage() != null) {
            return response.getProvinceDtoPage().getContent();
        }

        // Return an empty list if no data is found or if there's an error to prevent downstream NullPointerExceptions.
        return new ArrayList<>();
    }

    @Override
    public ImportSummary importFromCsv(InputStream csvInputStream) throws IOException {
        logger.info("Incoming request to import provinces from CSV");
        ImportSummary importSummary = provinceService.importProvinceFromCsv(csvInputStream);
        logger.info("Outgoing response after importing provinces from CSV: Total: {}, Success: {}, Failed: {}",
                importSummary.total, importSummary.success, importSummary.failed);
        return importSummary;
    }
}
