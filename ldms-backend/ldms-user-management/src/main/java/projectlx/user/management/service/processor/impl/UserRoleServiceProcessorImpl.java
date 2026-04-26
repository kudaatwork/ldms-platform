package projectlx.user.management.service.processor.impl;

import com.lowagie.text.DocumentException;
import org.springframework.data.domain.Page;
import projectlx.user.management.business.logic.api.UserRoleService;
import projectlx.user.management.service.processor.api.UserRoleServiceProcessor;
import projectlx.user.management.utils.dtos.ImportSummary;
import projectlx.user.management.utils.dtos.UserRoleDto;
import projectlx.user.management.utils.requests.CreateUserRoleRequest;
import projectlx.user.management.utils.requests.EditUserRoleRequest;
import projectlx.user.management.utils.requests.UserRoleMultipleFiltersRequest;
import projectlx.user.management.utils.responses.UserRoleResponse;
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
public class UserRoleServiceProcessorImpl implements UserRoleServiceProcessor {

    private final UserRoleService userRoleService;
    private final Logger logger = LoggerFactory.getLogger(UserRoleServiceProcessorImpl.class);

    @Override
    public UserRoleResponse create(CreateUserRoleRequest createUserRoleRequest, Locale locale, String username) {

        logger.info("Incoming request to create a user role : {}", createUserRoleRequest);

        UserRoleResponse userRoleResponse = userRoleService.create(createUserRoleRequest,
                locale, username);

        logger.info("Outgoing response after creating a user role : {}. Status Code: {}. Message: {}",
                userRoleResponse, userRoleResponse.getStatusCode(), userRoleResponse.getMessage());

        return userRoleResponse;
    }

    @Override
    public UserRoleResponse findById(Long id, Locale locale, String username) {

        logger.info("Incoming request to find a user role by id: {}", id);

        UserRoleResponse userRoleResponse = userRoleService.findById(id, locale, username);

        logger.info("Outgoing response after finding a user role by id : {}. Status Code: {}. Message: {}",
                userRoleResponse, userRoleResponse.getStatusCode(), userRoleResponse.getMessage());

        return userRoleResponse;
    }

    @Override
    public UserRoleResponse findAllAsList(String username, Locale locale) {

        logger.info("Incoming request to find all user roles as a list");

        UserRoleResponse userRoleResponse = userRoleService.findAllAsList(username, locale);

        logger.info("Outgoing response after finding all user roles as a list : {}. Status Code: {}. Message: {}",
                userRoleResponse, userRoleResponse.getStatusCode(), userRoleResponse.getMessage());

        return userRoleResponse;
    }

    @Override
    public UserRoleResponse update(EditUserRoleRequest editUserRoleRequest, String username, Locale locale) {

        logger.info("Incoming request to update a user role : {}", editUserRoleRequest);

        UserRoleResponse userRoleResponse = userRoleService.update(editUserRoleRequest, username, locale);

        logger.info("Outgoing response after updating a user role : {}. Status Code: {}. Message: {}",
                userRoleResponse, userRoleResponse.getStatusCode(), userRoleResponse.getMessage());

        return userRoleResponse;
    }

    @Override
    public UserRoleResponse delete(Long id, Locale locale, String username) {

        logger.info("Incoming request to delete a user role with the id : {}", id);

        UserRoleResponse userRoleResponse = userRoleService.delete(id, locale, username);

        logger.info("Outgoing response after deleting a user role: {}. Status Code: {}. Message: {}", userRoleResponse,
                userRoleResponse.getStatusCode(), userRoleResponse.getMessage());

        return userRoleResponse;
    }

    @Override
    public UserRoleResponse findByMultipleFilters(UserRoleMultipleFiltersRequest userRoleMultipleFiltersRequest, String username, Locale locale) {

        logger.info("Incoming request to find a user role using multiple filters : {}", userRoleMultipleFiltersRequest);

        UserRoleResponse userRoleResponse = userRoleService.findByMultipleFilters(userRoleMultipleFiltersRequest,
                username, locale);

        logger.info("Outgoing response after finding a user role using multiple filters: {}. Status Code: {}. Message: {}",
                userRoleResponse, userRoleResponse.getStatusCode(), userRoleResponse.getMessage());

        return userRoleResponse;
    }

    @Override
    public byte[] exportToCsv(UserRoleMultipleFiltersRequest filters, String username, Locale locale) {
        logger.info("Incoming request to export user roles to CSV using filters: {}", filters);

        UserRoleResponse userRoleResponse = userRoleService.findByMultipleFilters(filters, username, locale);

        List<UserRoleDto> userRoleList = Optional.ofNullable(userRoleResponse.getUserRoleDtoPage())
                .map(Page::getContent)
                .orElse(Collections.emptyList());

        byte[] csvData = userRoleService.exportToCsv(userRoleList);

        logger.info("Outgoing CSV export complete. Byte size: {}", csvData.length);

        return csvData;
    }

    @Override
    public byte[] exportToExcel(UserRoleMultipleFiltersRequest filters, String username, Locale locale) throws IOException {
        logger.info("Incoming request to export user roles to Excel using filters: {}", filters);

        UserRoleResponse userRoleResponse = userRoleService.findByMultipleFilters(filters, username, locale);

        List<UserRoleDto> userRoleList = Optional.ofNullable(userRoleResponse.getUserRoleDtoPage())
                .map(Page::getContent)
                .orElse(Collections.emptyList());

        byte[] excelData = userRoleService.exportToExcel(userRoleList);

        logger.info("Outgoing Excel export complete. Byte size: {}", excelData.length);

        return excelData;
    }

    @Override
    public byte[] exportToPdf(UserRoleMultipleFiltersRequest filters, String username, Locale locale) throws DocumentException {
        logger.info("Incoming request to export user roles to PDF using filters: {}", filters);

        UserRoleResponse userRoleResponse = userRoleService.findByMultipleFilters(filters, username, locale);

        List<UserRoleDto> userRoleList = Optional.ofNullable(userRoleResponse.getUserRoleDtoPage())
                .map(Page::getContent)
                .orElse(Collections.emptyList());

        byte[] pdfData = userRoleService.exportToPdf(userRoleList);

        logger.info("Outgoing PDF export complete. Byte size: {}", pdfData.length);

        return pdfData;
    }

    @Override
    public ImportSummary importUserRolesFromCsv(InputStream csvInputStream) throws IOException {
        logger.info("Incoming request to import user roles from CSV");

        ImportSummary importSummary = userRoleService.importUserRolesFromCsv(csvInputStream);

        logger.info("Outgoing response after importing user roles from CSV. Status Code: {}. Message: {}",
                importSummary.getStatusCode(), importSummary.getMessage());

        return importSummary;
    }
}
