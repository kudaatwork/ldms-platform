package projectlx.user.management.service.service.processor.impl;

import com.lowagie.text.DocumentException;
import org.springframework.data.domain.Page;
import projectlx.user.management.service.business.logic.api.UserAccountService;
import projectlx.user.management.service.service.processor.api.UserAccountServiceProcessor;
import projectlx.user.management.service.utils.dtos.ImportSummary;
import projectlx.user.management.service.utils.dtos.UserAccountDto;
import projectlx.user.management.service.utils.requests.CreateUserAccountRequest;
import projectlx.user.management.service.utils.requests.EditUserAccountRequest;
import projectlx.user.management.service.utils.requests.UserAccountMultipleFiltersRequest;
import projectlx.user.management.service.utils.responses.UserAccountResponse;
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
public class UserAccountServiceProcessorImpl implements UserAccountServiceProcessor {

    private final UserAccountService userAccountService;
    private static final Logger logger = LoggerFactory.getLogger(UserAccountServiceProcessorImpl.class);

    @Override
    public UserAccountResponse create(CreateUserAccountRequest createUserAccountRequest, Locale locale, String username) {

        logger.info("Incoming request to create a user account : {}", createUserAccountRequest);

        UserAccountResponse userAccountResponse = userAccountService.create(createUserAccountRequest,
                locale, username);

        logger.info("Outgoing response after creating a user account : {}. Status Code: {}. Message: {}",
                userAccountResponse, userAccountResponse.getStatusCode(), userAccountResponse.getMessage());

        return userAccountResponse;
    }

    @Override
    public UserAccountResponse findById(Long id, Locale locale, String username) {

        logger.info("Incoming request to find a user account by id: {}", id);

        UserAccountResponse userAccountResponse = userAccountService.findById(id, locale, username);

        logger.info("Outgoing response after finding a user create by id : {}. Status Code: {}. Message: {}",
                userAccountResponse, userAccountResponse.getStatusCode(), userAccountResponse.getMessage());

        return userAccountResponse;
    }

    @Override
    public UserAccountResponse findAllAsList(String username, Locale locale) {

        logger.info("Incoming request to find all user account as a list");

        UserAccountResponse userAccountResponse = userAccountService.findAllAsList(username, locale);

        logger.info("Outgoing response after finding all user account as a list : {}. Status Code: {}. Message: {}",
                userAccountResponse, userAccountResponse.getStatusCode(), userAccountResponse.getMessage());

        return userAccountResponse;
    }

    @Override
    public UserAccountResponse update(EditUserAccountRequest editUserAccountRequest, String username, Locale locale) {

        logger.info("Incoming request to update a user account : {}", editUserAccountRequest);

        UserAccountResponse userAccountResponse = userAccountService.update(editUserAccountRequest, username, locale);

        logger.info("Outgoing response after updating a user account : {}. Status Code: {}. Message: {}",
                userAccountResponse, userAccountResponse.getStatusCode(), userAccountResponse.getMessage());

        return userAccountResponse;
    }

    @Override
    public UserAccountResponse delete(Long id, Locale locale, String username) {

        logger.info("Incoming request to delete a user account with the id : {}", id);

        UserAccountResponse userAccountResponse = userAccountService.delete(id, locale, username);

        logger.info("Outgoing response after deleting a user account: {}. Status Code: {}. Message: {}", userAccountResponse,
                userAccountResponse.getStatusCode(), userAccountResponse.getMessage());

        return userAccountResponse;
    }

    @Override
    public UserAccountResponse findByMultipleFilters(UserAccountMultipleFiltersRequest userAccountMultipleFiltersRequest,
                                                     String username, Locale locale) {

        logger.info("Incoming request to find a user account using multiple filters : {}", userAccountMultipleFiltersRequest);

        UserAccountResponse userAccountResponse = userAccountService.findByMultipleFilters(userAccountMultipleFiltersRequest,
                username, locale);

        logger.info("Outgoing response after finding a user account using multiple filters: {}. Status Code: {}. Message: {}",
                userAccountResponse, userAccountResponse.getStatusCode(), userAccountResponse.getMessage());

        return userAccountResponse;
    }

    @Override
    public ImportSummary importUserAccountsFromCsv(InputStream csvInputStream) throws IOException {
        logger.info("Incoming request to import user accounts from CSV");

        ImportSummary importSummary = userAccountService.importUserAccountsFromCsv(csvInputStream);

        logger.info("Outgoing response after importing user accounts from CSV: Total: {}, Success: {}, Failed: {}",
                importSummary.total, importSummary.success, importSummary.failed);

        return importSummary;
    }

    @Override
    public ImportSummary importUserAccountsFromExcel(InputStream excelInputStream) throws IOException {
        logger.info("Incoming request to import user accounts from Excel");

        ImportSummary importSummary = userAccountService.importUserAccountsFromExcel(excelInputStream);

        logger.info("Outgoing response after importing user accounts from Excel: Total: {}, Success: {}, Failed: {}",
                importSummary.total, importSummary.success, importSummary.failed);

        return importSummary;
    }

    @Override
    public byte[] exportToCsv(UserAccountMultipleFiltersRequest filters, String username, Locale locale) {
        logger.info("Incoming request to export user accounts to CSV using filters: {}", filters);

        UserAccountResponse userAccountResponse = userAccountService.findByMultipleFilters(filters, username, locale);

        List<UserAccountDto> userAccountList = Optional.ofNullable(userAccountResponse.getUserAccountDtoPage())
                .map(Page::getContent)
                .orElse(Collections.emptyList());

        byte[] csvData = userAccountService.exportToCsv(userAccountList);

        logger.info("Outgoing CSV export complete. Byte size: {}", csvData.length);

        return csvData;
    }

    @Override
    public byte[] exportToExcel(UserAccountMultipleFiltersRequest filters, String username, Locale locale) throws IOException {
        logger.info("Incoming request to export user accounts to Excel using filters: {}", filters);

        UserAccountResponse userAccountResponse = userAccountService.findByMultipleFilters(filters, username, locale);

        List<UserAccountDto> userAccountList = Optional.ofNullable(userAccountResponse.getUserAccountDtoPage())
                .map(Page::getContent)
                .orElse(Collections.emptyList());

        byte[] excelData = userAccountService.exportToExcel(userAccountList);

        logger.info("Outgoing Excel export complete. Byte size: {}", excelData.length);

        return excelData;
    }

    @Override
    public byte[] exportToPdf(UserAccountMultipleFiltersRequest filters, String username, Locale locale) throws DocumentException {
        logger.info("Incoming request to export user accounts to PDF using filters: {}", filters);

        UserAccountResponse userAccountResponse = userAccountService.findByMultipleFilters(filters, username, locale);

        List<UserAccountDto> userAccountList = Optional.ofNullable(userAccountResponse.getUserAccountDtoPage())
                .map(Page::getContent)
                .orElse(Collections.emptyList());

        byte[] pdfData = userAccountService.exportToPdf(userAccountList);

        logger.info("Outgoing PDF export complete. Byte size: {}", pdfData.length);

        return pdfData;
    }

}
