package projectlx.co.zw.locationsmanagementservice.service.processor.impl;

import com.lowagie.text.DocumentException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import projectlx.co.zw.locationsmanagementservice.business.logic.api.GeoCoordinatesService;
import projectlx.co.zw.locationsmanagementservice.service.processor.api.GeoCoordinatesServiceProcessor;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.GeoCoordinatesDto;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.ImportSummary;
import projectlx.co.zw.locationsmanagementservice.utils.requests.CreateGeoCoordinatesRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.EditGeoCoordinatesRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.GeoCoordinatesMultipleFiltersRequest;
import projectlx.co.zw.locationsmanagementservice.utils.responses.GeoCoordinatesResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class GeoCoordinatesServiceProcessorImpl implements GeoCoordinatesServiceProcessor {

    private final GeoCoordinatesService geoCoordinatesService;
    private final Logger logger = LoggerFactory.getLogger(GeoCoordinatesServiceProcessorImpl.class);

    @Override
    public GeoCoordinatesResponse create(CreateGeoCoordinatesRequest request, Locale locale, String username) {

        logger.info("Incoming request to create geo coordinates : {}", request);

        GeoCoordinatesResponse geoCoordinatesResponse = geoCoordinatesService.create(request, locale, username);

        logger.info("Outgoing response after creating geo coordinates : {}. Status Code: {}. Message: {}",
                geoCoordinatesResponse, geoCoordinatesResponse.getStatusCode(), geoCoordinatesResponse.getMessage());

        return geoCoordinatesResponse;
    }

    @Override
    public GeoCoordinatesResponse update(EditGeoCoordinatesRequest request, Locale locale, String username) {
        logger.info("Incoming request to update geo coordinates : {}", request);

        GeoCoordinatesResponse geoCoordinatesResponse = geoCoordinatesService.update(request, username, locale);

        logger.info("Outgoing response after updating geo coordinates : {}. Status Code: {}. Message: {}",
                geoCoordinatesResponse, geoCoordinatesResponse.getStatusCode(), geoCoordinatesResponse.getMessage());

        return geoCoordinatesResponse;
    }

    @Override
    public GeoCoordinatesResponse findById(Long id, Locale locale, String username) {
        logger.info("Incoming request to find geo coordinates by id: {}", id);

        GeoCoordinatesResponse geoCoordinatesResponse = geoCoordinatesService.findById(id, locale, username);

        logger.info("Outgoing response after finding geo coordinates by id: {}. Status Code: {}. Message: {}",
                geoCoordinatesResponse, geoCoordinatesResponse.getStatusCode(), geoCoordinatesResponse.getMessage());

        return geoCoordinatesResponse;
    }

    @Override
    public GeoCoordinatesResponse findAll(Locale locale, String username) {
        logger.info("Incoming request to find all geo coordinates");

        GeoCoordinatesResponse geoCoordinatesResponse = geoCoordinatesService.findAllAsList(locale, username);

        logger.info("Outgoing response after finding all geo coordinates. Status Code: {}. Message: {}",
                geoCoordinatesResponse.getStatusCode(), geoCoordinatesResponse.getMessage());

        return geoCoordinatesResponse;
    }

    @Override
    public GeoCoordinatesResponse delete(Long id, Locale locale, String username) {
        logger.info("Incoming request to delete geo coordinates with the id : {}", id);

        GeoCoordinatesResponse geoCoordinatesResponse = geoCoordinatesService.delete(id, locale, username);

        logger.info("Outgoing response after deleting geo coordinates: {}. Status Code: {}. Message: {}", 
                geoCoordinatesResponse, geoCoordinatesResponse.getStatusCode(), geoCoordinatesResponse.getMessage());

        return geoCoordinatesResponse;
    }

    @Override
    public GeoCoordinatesResponse findByMultipleFilters(GeoCoordinatesMultipleFiltersRequest request, String username, Locale locale) {
        logger.info("Incoming request to find geo coordinates by multiple filters: {}", request);

        GeoCoordinatesResponse geoCoordinatesResponse = geoCoordinatesService.findByMultipleFilters(request, username, locale);

        logger.info("Outgoing response after finding geo coordinates by multiple filters. Status Code: {}. Message: {}",
                geoCoordinatesResponse.getStatusCode(), geoCoordinatesResponse.getMessage());

        return geoCoordinatesResponse;
    }

    @Override
    public byte[] exportToCsv(List<GeoCoordinatesDto> dtoList) {
        logger.info("Incoming request to export geo coordinates to CSV");

        byte[] csvData = geoCoordinatesService.exportToCsv(dtoList);

        logger.info("Outgoing response after exporting geo coordinates to CSV");

        return csvData;
    }

    @Override
    public byte[] exportToExcel(List<GeoCoordinatesDto> dtoList) throws IOException {
        logger.info("Incoming request to export geo coordinates to Excel");

        byte[] excelData = geoCoordinatesService.exportToExcel(dtoList);

        logger.info("Outgoing response after exporting geo coordinates to Excel");

        return excelData;
    }

    @Override
    public byte[] exportToPdf(List<GeoCoordinatesDto> dtoList) throws DocumentException {
        logger.info("Incoming request to export geo coordinates to PDF");

        byte[] pdfData = geoCoordinatesService.exportToPdf(dtoList);

        logger.info("Outgoing response after exporting geo coordinates to PDF");

        return pdfData;
    }

    @Override
    public ImportSummary importFromCsv(InputStream csvInputStream) throws IOException {
        logger.info("Incoming request to import geo coordinates from CSV");

        ImportSummary importSummary = geoCoordinatesService.importGeoCoordinatesFromCsv(csvInputStream);

        logger.info("Outgoing response after importing geo coordinates from CSV. Status Code: {}. Message: {}",
                importSummary.getStatusCode(), importSummary.getMessage());

        return importSummary;
    }
}
