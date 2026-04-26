package projectlx.user.management.service.processor.impl;

import com.lowagie.text.DocumentException;
import org.springframework.data.domain.Page;
import projectlx.user.management.business.logic.api.UserTypeService;
import projectlx.user.management.service.processor.api.UserTypeServiceProcessor;
import projectlx.user.management.utils.dtos.ImportSummary;
import projectlx.user.management.utils.dtos.UserTypeDto;
import projectlx.user.management.utils.requests.CreateUserTypeRequest;
import projectlx.user.management.utils.requests.EditUserTypeRequest;
import projectlx.user.management.utils.requests.UserTypeMultipleFiltersRequest;
import projectlx.user.management.utils.responses.UserTypeResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@RequiredArgsConstructor
public class UserTypeServiceProcessorImpl implements UserTypeServiceProcessor {

    private final UserTypeService userTypeService;
    private final Logger logger = LoggerFactory.getLogger(UserTypeServiceProcessorImpl.class);

    @Override
    public UserTypeResponse create(CreateUserTypeRequest createUserTypeRequest, Locale locale, String username) {

        logger.info("Incoming request to create a user type : {}", createUserTypeRequest);

        UserTypeResponse userTypeResponse = userTypeService.create(createUserTypeRequest,
                locale, username);

        logger.info("Outgoing response after creating a user type : {}. Status Code: {}. Message: {}",
                userTypeResponse, userTypeResponse.getStatusCode(), userTypeResponse.getMessage());

        return userTypeResponse;
    }

    @Override
    public UserTypeResponse findById(Long id, Locale locale, String username) {

        logger.info("Incoming request to find a user type by id: {}", id);

        UserTypeResponse userTypeResponse = userTypeService.findById(id, locale, username);

        logger.info("Outgoing response after finding a user type by id : {}. Status Code: {}. Message: {}",
                userTypeResponse, userTypeResponse.getStatusCode(), userTypeResponse.getMessage());

        return userTypeResponse;
    }

    @Override
    public UserTypeResponse findAllAsList(String username, Locale locale) {

        logger.info("Incoming request to find all user types as a list");

        UserTypeResponse userTypeResponse = userTypeService.findAllAsList(username, locale);

        logger.info("Outgoing response after finding all user types as a list : {}. Status Code: {}. Message: {}",
                userTypeResponse, userTypeResponse.getStatusCode(), userTypeResponse.getMessage());

        return userTypeResponse;
    }

    @Override
    public UserTypeResponse update(EditUserTypeRequest editUserTypeRequest, String username, Locale locale) {

        logger.info("Incoming request to update a user type : {}", editUserTypeRequest);

        UserTypeResponse userTypeResponse = userTypeService.update(editUserTypeRequest, username, locale);

        logger.info("Outgoing response after updating a user type : {}. Status Code: {}. Message: {}",
                userTypeResponse, userTypeResponse.getStatusCode(), userTypeResponse.getMessage());

        return userTypeResponse;
    }

    @Override
    public UserTypeResponse delete(Long id, Locale locale, String username) {

        logger.info("Incoming request to delete a user type with the id : {}", id);

        UserTypeResponse userTypeResponse = userTypeService.delete(id, locale, username);

        logger.info("Outgoing response after deleting a user type: {}. Status Code: {}. Message: {}", userTypeResponse,
                userTypeResponse.getStatusCode(), userTypeResponse.getMessage());

        return userTypeResponse;
    }

    @Override
    public UserTypeResponse findByMultipleFilters(UserTypeMultipleFiltersRequest userTypeMultipleFiltersRequest, String username, Locale locale) {

        logger.info("Incoming request to find a user type using multiple filters : {}", userTypeMultipleFiltersRequest);

        UserTypeResponse userTypeResponse = userTypeService.findByMultipleFilters(userTypeMultipleFiltersRequest,
                username, locale);

        logger.info("Outgoing response after finding a user type using multiple filters: {}. Status Code: {}. Message: {}",
                userTypeResponse, userTypeResponse.getStatusCode(), userTypeResponse.getMessage());

        return userTypeResponse;
    }

    @Override
    public byte[] exportToCsv(UserTypeMultipleFiltersRequest filters, String username, Locale locale) {
        logger.info("Incoming request to export user types to CSV using filters: {}", filters);

        UserTypeResponse userTypeResponse = userTypeService.findByMultipleFilters(filters, username, locale);

        List<UserTypeDto> userTypeList = Optional.ofNullable(userTypeResponse.getUserTypeDtoPage())
                .map(Page::getContent)
                .orElse(Collections.emptyList());

        byte[] csvData = userTypeService.exportToCsv(userTypeList);

        logger.info("Outgoing CSV export complete. Byte size: {}", csvData.length);

        return csvData;
    }

    @Override
    public byte[] exportToExcel(UserTypeMultipleFiltersRequest filters, String username, Locale locale) throws IOException {
        logger.info("Incoming request to export user types to Excel using filters: {}", filters);

        UserTypeResponse userTypeResponse = userTypeService.findByMultipleFilters(filters, username, locale);

        List<UserTypeDto> userTypeList = Optional.ofNullable(userTypeResponse.getUserTypeDtoPage())
                .map(Page::getContent)
                .orElse(Collections.emptyList());

        byte[] excelData = userTypeService.exportToExcel(userTypeList);

        logger.info("Outgoing Excel export complete. Byte size: {}", excelData.length);

        return excelData;
    }

    @Override
    public byte[] exportToPdf(UserTypeMultipleFiltersRequest filters, String username, Locale locale) throws DocumentException {
        logger.info("Incoming request to export user types to PDF using filters: {}", filters);

        UserTypeResponse userTypeResponse = userTypeService.findByMultipleFilters(filters, username, locale);

        List<UserTypeDto> userTypeList = Optional.ofNullable(userTypeResponse.getUserTypeDtoPage())
                .map(Page::getContent)
                .orElse(Collections.emptyList());

        byte[] pdfData = userTypeService.exportToPdf(userTypeList);

        logger.info("Outgoing PDF export complete. Byte size: {}", pdfData.length);

        return pdfData;
    }

    @Override
    public ImportSummary importUserTypesFromCsv(InputStream csvInputStream) throws IOException {
        logger.info("Incoming request to import user types from CSV");

        ImportSummary summary = userTypeService.importUserTypesFromCsv(csvInputStream);

        logger.info("Outgoing response after importing user types from CSV. Status Code: {}. Success: {}. Message: {}. Total: {}. Success: {}. Failed: {}",
                summary.getStatusCode(), summary.isSuccess(), summary.getMessage(), summary.total, summary.success, summary.failed);

        return summary;
    }
}
