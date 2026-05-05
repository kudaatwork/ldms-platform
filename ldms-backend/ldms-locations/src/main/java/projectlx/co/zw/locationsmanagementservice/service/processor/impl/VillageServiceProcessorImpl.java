package projectlx.co.zw.locationsmanagementservice.service.processor.impl;

import com.lowagie.text.DocumentException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import projectlx.co.zw.locationsmanagementservice.business.logic.api.VillageService;
import projectlx.co.zw.locationsmanagementservice.service.processor.api.VillageServiceProcessor;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.ImportSummary;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.VillageDto;
import projectlx.co.zw.locationsmanagementservice.utils.requests.CreateVillageRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.EditVillageRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.VillageMultipleFiltersRequest;
import projectlx.co.zw.locationsmanagementservice.utils.responses.VillageResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

@RequiredArgsConstructor
public class VillageServiceProcessorImpl implements VillageServiceProcessor {

    private final VillageService villageService;
    private final Logger logger = LoggerFactory.getLogger(VillageServiceProcessorImpl.class);

    @Override
    public VillageResponse create(CreateVillageRequest request, Locale locale, String username) {
        logger.info("Incoming request to create a village : {}", request);
        VillageResponse response = villageService.create(request, locale, username);
        logger.info("Outgoing response after creating a village : {}. Status Code: {}. Message: {}",
                response, response.getStatusCode(), response.getMessage());
        return response;
    }

    @Override
    public VillageResponse update(EditVillageRequest request, Locale locale, String username) {
        logger.info("Incoming request to update a village : {}", request);
        VillageResponse response = villageService.update(request, username, locale);
        logger.info("Outgoing response after updating a village : {}. Status Code: {}. Message: {}",
                response, response.getStatusCode(), response.getMessage());
        return response;
    }

    @Override
    public VillageResponse findById(Long id, Locale locale, String username) {
        logger.info("Incoming request to find a village by id: {}", id);
        VillageResponse response = villageService.findById(id, locale, username);
        logger.info("Outgoing response after finding a village by id : {}. Status Code: {}. Message: {}",
                response, response.getStatusCode(), response.getMessage());
        return response;
    }

    @Override
    public VillageResponse findAll(Locale locale, String username) {
        logger.info("Incoming request to find all villages as a list");
        VillageResponse response = villageService.findAllAsList(locale, username);
        logger.info("Outgoing response after finding all villages as a list : {}. Status Code: {}. Message: {}",
                response, response.getStatusCode(), response.getMessage());
        return response;
    }

    @Override
    public VillageResponse delete(Long id, Locale locale, String username) {
        logger.info("Incoming request to delete a village with the id : {}", id);
        VillageResponse response = villageService.delete(id, locale, username);
        logger.info("Outgoing response after deleting a village: {}. Status Code: {}. Message: {}", response,
                response.getStatusCode(), response.getMessage());
        return response;
    }

    @Override
    public VillageResponse findByMultipleFilters(VillageMultipleFiltersRequest request, String username, Locale locale) {
        logger.info("Incoming request to find villages using multiple filters : {}", request);
        VillageResponse response = villageService.findByMultipleFilters(request, username, locale);
        logger.info("Outgoing response after finding villages using multiple filters: {}. Status Code: {}. Message: {}",
                response, response.getStatusCode(), response.getMessage());
        return response;
    }

    @Override
    public byte[] exportToCsv(List<VillageDto> dtoList) {
        return villageService.exportToCsv(dtoList);
    }

    @Override
    public byte[] exportToExcel(List<VillageDto> dtoList) throws IOException {
        return villageService.exportToExcel(dtoList);
    }

    @Override
    public byte[] exportToPdf(List<VillageDto> dtoList) throws DocumentException {
        return villageService.exportToPdf(dtoList);
    }

    @Override
    public ImportSummary importFromCsv(InputStream csvInputStream) throws IOException {
        return villageService.importVillageFromCsv(csvInputStream);
    }
}
