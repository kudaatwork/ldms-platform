package projectlx.co.zw.locationsmanagementservice.service.processor.impl;

import com.lowagie.text.DocumentException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import projectlx.co.zw.locationsmanagementservice.business.logic.api.CountryService;
import projectlx.co.zw.locationsmanagementservice.service.processor.api.CountryServiceProcessor;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.CountryDto;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.ImportSummary;
import projectlx.co.zw.locationsmanagementservice.utils.requests.CreateCountryRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.EditCountryRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.CountryMultipleFiltersRequest;
import projectlx.co.zw.locationsmanagementservice.utils.responses.CountryResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

@RequiredArgsConstructor
public class CountryServiceProcessorImpl implements CountryServiceProcessor {

    private final CountryService countryService;
    private final Logger logger = LoggerFactory.getLogger(CountryServiceProcessorImpl.class);

    @Override
    public CountryResponse create(CreateCountryRequest request, Locale locale, String username) {
        logger.info("Incoming request to create a country : {}", request);

        CountryResponse countryResponse = countryService.create(request, locale, username);

        logger.info("Outgoing response after creating a country : {}. Status Code: {}. Message: {}",
                countryResponse, countryResponse.getStatusCode(), countryResponse.getMessage());

        return countryResponse;
    }

    @Override
    public CountryResponse update(EditCountryRequest request, Locale locale, String username) {
        logger.info("Incoming request to update a country : {}", request);

        CountryResponse countryResponse = countryService.update(request, username, locale);

        logger.info("Outgoing response after updating a country : {}. Status Code: {}. Message: {}",
                countryResponse, countryResponse.getStatusCode(), countryResponse.getMessage());

        return countryResponse;
    }

    @Override
    public CountryResponse findById(Long id, Locale locale, String username) {
        logger.info("Incoming request to find a country by id: {}", id);

        CountryResponse countryResponse = countryService.findById(id, locale, username);

        logger.info("Outgoing response after finding a country by id : {}. Status Code: {}. Message: {}",
                countryResponse, countryResponse.getStatusCode(), countryResponse.getMessage());

        return countryResponse;
    }

    @Override
    public CountryResponse findAll(Locale locale, String username) {
        logger.info("Incoming request to find all countries as a list");

        CountryResponse countryResponse = countryService.findAllAsList(locale, username);

        logger.info("Outgoing response after finding all countries as a list : {}. Status Code: {}. Message: {}",
                countryResponse, countryResponse.getStatusCode(), countryResponse.getMessage());

        return countryResponse;
    }

    @Override
    public CountryResponse delete(Long id, Locale locale, String username) {
        logger.info("Incoming request to delete a country with the id : {}", id);

        CountryResponse countryResponse = countryService.delete(id, locale, username);

        logger.info("Outgoing response after deleting a country: {}. Status Code: {}. Message: {}", countryResponse,
                countryResponse.getStatusCode(), countryResponse.getMessage());

        return countryResponse;
    }

    @Override
    public CountryResponse findByMultipleFilters(CountryMultipleFiltersRequest request, String username, Locale locale) {
        logger.info("Incoming request to find a country using multiple filters : {}", request);

        CountryResponse countryResponse = countryService.findByMultipleFilters(request, username, locale);

        logger.info("Outgoing response after finding a country using multiple filters: {}. Status Code: {}. Message: {}",
                countryResponse, countryResponse.getStatusCode(), countryResponse.getMessage());

        return countryResponse;
    }

    @Override
    public byte[] exportToCsv(List<CountryDto> dtoList) {
        logger.info("Incoming request to export countries to CSV. List size: {}", dtoList.size());

        byte[] csvData = countryService.exportToCsv(dtoList);

        logger.info("Outgoing CSV export complete. Byte size: {}", csvData.length);

        return csvData;
    }

    @Override
    public byte[] exportToExcel(List<CountryDto> dtoList) throws IOException {
        logger.info("Incoming request to export countries to Excel. List size: {}", dtoList.size());

        byte[] excelData = countryService.exportToExcel(dtoList);

        logger.info("Outgoing Excel export complete. Byte size: {}", excelData.length);

        return excelData;
    }

    @Override
    public byte[] exportToPdf(List<CountryDto> dtoList) throws DocumentException {
        logger.info("Incoming request to export countries to PDF. List size: {}", dtoList.size());

        byte[] pdfData = countryService.exportToPdf(dtoList);

        logger.info("Outgoing PDF export complete. Byte size: {}", pdfData.length);

        return pdfData;
    }

    @Override
    public ImportSummary importFromCsv(InputStream csvInputStream) throws IOException {
        logger.info("Incoming request to import countries from CSV");

        ImportSummary importSummary = countryService.importCountryFromCsv(csvInputStream);

        logger.info("Outgoing response after importing countries from CSV: Total: {}, Success: {}, Failed: {}",
                importSummary.total, importSummary.importedCount, importSummary.failed);

        return importSummary;
    }
}
