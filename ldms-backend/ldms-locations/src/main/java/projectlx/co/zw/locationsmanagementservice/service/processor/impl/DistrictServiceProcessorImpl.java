package projectlx.co.zw.locationsmanagementservice.service.processor.impl;

import com.lowagie.text.DocumentException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import projectlx.co.zw.locationsmanagementservice.business.logic.api.DistrictService;
import projectlx.co.zw.locationsmanagementservice.service.processor.api.DistrictServiceProcessor;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.DistrictDto;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.ImportSummary;
import projectlx.co.zw.locationsmanagementservice.utils.requests.CreateDistrictRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.EditDistrictRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.DistrictMultipleFiltersRequest;
import projectlx.co.zw.locationsmanagementservice.utils.responses.DistrictResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

@RequiredArgsConstructor
public class DistrictServiceProcessorImpl implements DistrictServiceProcessor {

    private final DistrictService districtService;
    private final Logger logger = LoggerFactory.getLogger(DistrictServiceProcessorImpl.class);

    @Override
    public DistrictResponse    create(CreateDistrictRequest request, Locale locale, String username) {
        logger.info("Incoming request to create a district : {}", request);

        DistrictResponse districtResponse = districtService.create(request, locale, username);

        logger.info("Outgoing response after creating a district : {}. Status Code: {}. Message: {}",
                districtResponse, districtResponse.getStatusCode(), districtResponse.getMessage());

        return districtResponse;
    }

    @Override
    public DistrictResponse update(EditDistrictRequest request, Locale locale, String username) {
        logger.info("Incoming request to update a district : {}", request);

        DistrictResponse districtResponse = districtService.update(request, username, locale);

        logger.info("Outgoing response after updating a district : {}. Status Code: {}. Message: {}",
                districtResponse, districtResponse.getStatusCode(), districtResponse.getMessage());

        return districtResponse;
    }

    @Override
    public DistrictResponse findById(Long id, Locale locale, String username) {
        logger.info("Incoming request to find a district by id: {}", id);

        DistrictResponse districtResponse = districtService.findById(id, locale, username);

        logger.info("Outgoing response after finding a district by id : {}. Status Code: {}. Message: {}",
                districtResponse, districtResponse.getStatusCode(), districtResponse.getMessage());

        return districtResponse;
    }

    @Override
    public DistrictResponse findAll(Locale locale, String username) {
        logger.info("Incoming request to find all districts as a list");

        DistrictResponse districtResponse = districtService.findAllAsList(locale, username);

        logger.info("Outgoing response after finding all districts as a list : {}. Status Code: {}. Message: {}",
                districtResponse, districtResponse.getStatusCode(), districtResponse.getMessage());

        return districtResponse;
    }

    @Override
    public DistrictResponse delete(Long id, Locale locale, String username) {
        logger.info("Incoming request to delete a district with the id : {}", id);

        DistrictResponse districtResponse = districtService.delete(id, locale, username);

        logger.info("Outgoing response after deleting a district: {}. Status Code: {}. Message: {}", districtResponse,
                districtResponse.getStatusCode(), districtResponse.getMessage());

        return districtResponse;
    }

    @Override
    public DistrictResponse findByMultipleFilters(DistrictMultipleFiltersRequest request, String username, Locale locale) {
        logger.info("Incoming request to find a district using multiple filters : {}", request);

        DistrictResponse districtResponse = districtService.findByMultipleFilters(request, username, locale);

        logger.info("Outgoing response after finding a district using multiple filters: {}. Status Code: {}. Message: {}",
                districtResponse, districtResponse.getStatusCode(), districtResponse.getMessage());

        return districtResponse;
    }

    @Override
    public byte[] exportToCsv(List<DistrictDto> dtoList) {
        logger.info("Incoming request to export districts to CSV. List size: {}", dtoList.size());

        byte[] csvData = districtService.exportToCsv(dtoList);

        logger.info("Outgoing CSV export complete. Byte size: {}", csvData.length);

        return csvData;
    }

    @Override
    public byte[] exportToExcel(List<DistrictDto> dtoList) throws IOException {
        logger.info("Incoming request to export districts to Excel. List size: {}", dtoList.size());

        byte[] excelData = districtService.exportToExcel(dtoList);

        logger.info("Outgoing Excel export complete. Byte size: {}", excelData.length);

        return excelData;
    }

    @Override
    public byte[] exportToPdf(List<DistrictDto> dtoList) throws DocumentException {
        logger.info("Incoming request to export districts to PDF. List size: {}", dtoList.size());

        byte[] pdfData = districtService.exportToPdf(dtoList);

        logger.info("Outgoing PDF export complete. Byte size: {}", pdfData.length);

        return pdfData;
    }

    @Override
    public ImportSummary importFromCsv(InputStream csvInputStream) throws IOException {
        logger.info("Incoming request to import districts from CSV");

        ImportSummary importSummary = districtService.importDistrictFromCsv(csvInputStream);

        logger.info("Outgoing response after importing districts from CSV: Total: {}, Success: {}, Failed: {}",
                importSummary.total, importSummary.success, importSummary.failed);

        return importSummary;
    }
}
