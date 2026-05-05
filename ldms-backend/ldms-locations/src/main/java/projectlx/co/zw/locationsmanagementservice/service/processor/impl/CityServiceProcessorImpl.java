package projectlx.co.zw.locationsmanagementservice.service.processor.impl;

import com.lowagie.text.DocumentException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import projectlx.co.zw.locationsmanagementservice.business.logic.api.CityService;
import projectlx.co.zw.locationsmanagementservice.service.processor.api.CityServiceProcessor;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.CityDto;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.ImportSummary;
import projectlx.co.zw.locationsmanagementservice.utils.requests.CityMultipleFiltersRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.CreateCityRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.EditCityRequest;
import projectlx.co.zw.locationsmanagementservice.utils.responses.CityResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

@RequiredArgsConstructor
public class CityServiceProcessorImpl implements CityServiceProcessor {

    private final CityService cityService;
    private final Logger logger = LoggerFactory.getLogger(CityServiceProcessorImpl.class);

    @Override
    public CityResponse create(CreateCityRequest request, Locale locale, String username) {
        logger.info("Incoming request to create a city : {}", request);
        CityResponse response = cityService.create(request, locale, username);
        logger.info("Outgoing response after creating a city : {}. Status Code: {}. Message: {}",
                response, response.getStatusCode(), response.getMessage());
        return response;
    }

    @Override
    public CityResponse update(EditCityRequest request, Locale locale, String username) {
        logger.info("Incoming request to update a city : {}", request);
        CityResponse response = cityService.update(request, username, locale);
        logger.info("Outgoing response after updating a city : {}. Status Code: {}. Message: {}",
                response, response.getStatusCode(), response.getMessage());
        return response;
    }

    @Override
    public CityResponse findById(Long id, Locale locale, String username) {
        logger.info("Incoming request to find a city by id: {}", id);
        CityResponse response = cityService.findById(id, locale, username);
        logger.info("Outgoing response after finding a city by id : {}. Status Code: {}. Message: {}",
                response, response.getStatusCode(), response.getMessage());
        return response;
    }

    @Override
    public CityResponse findAll(Locale locale, String username) {
        logger.info("Incoming request to find all cities as a list");
        CityResponse response = cityService.findAllAsList(locale, username);
        logger.info("Outgoing response after finding all cities as a list : {}. Status Code: {}. Message: {}",
                response, response.getStatusCode(), response.getMessage());
        return response;
    }

    @Override
    public CityResponse delete(Long id, Locale locale, String username) {
        logger.info("Incoming request to delete a city with the id : {}", id);
        CityResponse response = cityService.delete(id, locale, username);
        logger.info("Outgoing response after deleting a city: {}. Status Code: {}. Message: {}", response,
                response.getStatusCode(), response.getMessage());
        return response;
    }

    @Override
    public CityResponse findByMultipleFilters(CityMultipleFiltersRequest request, String username, Locale locale) {
        logger.info("Incoming request to find cities using multiple filters : {}", request);
        CityResponse response = cityService.findByMultipleFilters(request, username, locale);
        logger.info("Outgoing response after finding cities using multiple filters: {}. Status Code: {}. Message: {}",
                response, response.getStatusCode(), response.getMessage());
        return response;
    }

    @Override
    public byte[] exportToCsv(List<CityDto> dtoList) {
        return cityService.exportToCsv(dtoList);
    }

    @Override
    public byte[] exportToExcel(List<CityDto> dtoList) throws IOException {
        return cityService.exportToExcel(dtoList);
    }

    @Override
    public byte[] exportToPdf(List<CityDto> dtoList) throws DocumentException {
        return cityService.exportToPdf(dtoList);
    }

    @Override
    public ImportSummary importFromCsv(InputStream csvInputStream) throws IOException {
        return cityService.importCityFromCsv(csvInputStream);
    }
}
