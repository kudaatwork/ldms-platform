package projectlx.user.management.service.processor.impl;

import com.lowagie.text.DocumentException;
import org.springframework.data.domain.Page;
import projectlx.user.management.business.logic.api.UserService;
import projectlx.user.management.service.processor.api.UserServiceProcessor;
import projectlx.user.management.utils.dtos.ImportSummary;
import projectlx.user.management.utils.dtos.UserDto;
import projectlx.user.management.utils.requests.CreateUserRequest;
import projectlx.user.management.utils.requests.EditUserRequest;
import projectlx.user.management.utils.requests.ForgotPasswordRequest;
import projectlx.user.management.utils.requests.UsersMultipleFiltersRequest;
import projectlx.user.management.utils.responses.UserResponse;
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
public class UserServiceProcessorImpl implements UserServiceProcessor {

    private final UserService userService;
    private static final Logger logger = LoggerFactory.getLogger(UserServiceProcessorImpl.class);

    @Override
    public UserResponse create(CreateUserRequest createUserRequest, Locale locale, String username) {

        logger.info("Incoming request to create a user : {}", createUserRequest);

        UserResponse userResponse = userService.create(createUserRequest,
                locale, username);

        logger.info("Outgoing response after creating a user : {}. Status Code: {}. Message: {}", userResponse,
                userResponse.getStatusCode(), userResponse.getMessage());

        return userResponse;
    }

    @Override
    public UserResponse findById(Long id, Locale locale, String username) {

        logger.info("Incoming request to find a user by id: {}", id);

        UserResponse userResponse = userService.findById(id, locale, username);

        logger.info("Outgoing response after finding a user by id : {}. Status Code: {}. Message: {}", userResponse,
                userResponse.getStatusCode(), userResponse.getMessage());

        return userResponse;
    }

    @Override
    public UserResponse findByUsername(String username, Locale locale) {

        logger.info("Incoming request to find a user by username: {}", username);

        UserResponse userResponse = userService.findByUsername(username, locale);

        logger.info("Outgoing response after finding a user by username : {}. Status Code: {}. Message: {}", userResponse,
                userResponse.getStatusCode(), userResponse.getMessage());

        return userResponse;
    }

    @Override
    public UserResponse findAllAsList(Locale locale, String username) {

        logger.info("Incoming request to find all users as a list");

        UserResponse userResponse = userService.findAllAsList(locale, username);

        logger.info("Outgoing response after finding all users as a list : {}. Status Code: {}. Message: {}", userResponse,
                userResponse.getStatusCode(), userResponse.getMessage());

        return userResponse;
    }

    @Override
    public UserResponse update(EditUserRequest editUserRequest, String username, Locale locale) {

        logger.info("Incoming request to update a user : {}", editUserRequest);

        UserResponse userResponse = userService.update(editUserRequest, username, locale);

        logger.info("Outgoing response after updating a user: {}. Status Code: {}. Message: {}", userResponse,
                userResponse.getStatusCode(), userResponse.getMessage());

        return userResponse;
    }

    @Override
    public UserResponse delete(Long id, Locale locale, String username) {

        logger.info("Incoming request to delete a user with the id : {}", id);

        UserResponse userResponse = userService.delete(id, locale, username);

        logger.info("Outgoing response after deleting a user: {}. Status Code: {}. Message: {}", userResponse,
                userResponse.getStatusCode(), userResponse.getMessage());

        return userResponse;
    }

    @Override
    public UserResponse findByMultipleFilters(UsersMultipleFiltersRequest usersMultipleFiltersRequest,
                                              String username, Locale locale) {

        logger.info("Incoming request to find a user using multiple filters : {}", usersMultipleFiltersRequest);

        UserResponse userResponse = userService.findByMultipleFilters(usersMultipleFiltersRequest, username,
                locale);

        logger.info("Outgoing response after finding a user using multiple filters: {}. Status Code: {}. Message: {}", userResponse,
                userResponse.getStatusCode(), userResponse.getMessage());

        return userResponse;
    }

    @Override
    public UserResponse findByOrganizationId(Long organizationId, Locale locale, String username) {

        logger.info("Incoming request to find users by organization id: {}", organizationId);

        UserResponse userResponse = userService.findByOrganizationId(organizationId, locale, username);

        logger.info("Outgoing response after finding users by organization id: {}. Status Code: {}. Message: {}", userResponse,
                userResponse.getStatusCode(), userResponse.getMessage());

        return userResponse;
    }

    @Override
    public UserResponse findByBranchId(Long branchId, Locale locale, String username) {

        logger.info("Incoming request to find users by branch id: {}", branchId);

        UserResponse userResponse = userService.findByBranchId(branchId, locale, username);

        logger.info("Outgoing response after finding users by branch id: {}. Status Code: {}. Message: {}", userResponse,
                userResponse.getStatusCode(), userResponse.getMessage());

        return userResponse;
    }

    @Override
    public byte[] exportToCsv(UsersMultipleFiltersRequest filters, String username, Locale locale) {

        logger.info("Incoming request to export users to CSV using filters: {}", filters);

        UserResponse userResponse = userService.findByMultipleFilters(filters, username, locale);

        List<UserDto> userList = Optional.ofNullable(userResponse.getUserDtoPage())
                .map(Page::getContent)
                .orElse(Collections.emptyList());

        byte[] csvBytes = userService.exportToCsv(userList);

        logger.info("Outgoing CSV export complete. Byte size: {}", csvBytes.length);

        return csvBytes;
    }

    @Override
    public byte[] exportToExcel(UsersMultipleFiltersRequest filters, String username, Locale locale) throws IOException {

        logger.info("Incoming request to export users to Excel using filters: {}", filters);

        UserResponse userResponse = userService.findByMultipleFilters(filters, username, locale);

        List<UserDto> userList = Optional.ofNullable(userResponse.getUserDtoPage())
                .map(Page::getContent)
                .orElse(Collections.emptyList());

        byte[] excelBytes = userService.exportToExcel(userList);

        logger.info("Outgoing Excel export complete. Byte size: {}", excelBytes.length);

        return excelBytes;
    }

    @Override
    public byte[] exportToPdf(UsersMultipleFiltersRequest filters, String username, Locale locale) throws DocumentException {

        logger.info("Incoming request to export users to PDF using filters: {}", filters);

        UserResponse userResponse = userService.findByMultipleFilters(filters, username, locale);

        List<UserDto> userList = Optional.ofNullable(userResponse.getUserDtoPage())
                .map(Page::getContent)
                .orElse(Collections.emptyList());

        byte[] pdfBytes = userService.exportToPdf(userList);

        logger.info("Outgoing PDF export complete. Byte size: {}", pdfBytes.length);

        return pdfBytes;
    }

    @Override
    public ImportSummary importUsersFromCsv(InputStream csvInputStream) throws IOException {

        logger.info("Incoming request to import users from CSV");

        ImportSummary summary = userService.importUsersFromCsv(csvInputStream);

        logger.info("CSV import completed: total={}, success={}, failed={}",
                summary.total, summary.success, summary.failed);

        return summary;
    }

    @Override
    public ImportSummary importUsersFromExcel(InputStream excelInputStream) throws IOException {

        logger.info("Incoming request to import users from Excel");

        ImportSummary summary = userService.importUsersFromExcel(excelInputStream);

        logger.info("Excel import completed: total={}, success={}, failed={}",
                summary.total, summary.success, summary.failed);

        return summary;
    }

    @Override
    public UserResponse verifyEmail(String email, String token, Locale locale, String username) {

        logger.info("Incoming request to verify an email: {}", email);

        UserResponse userResponse = userService.verifyEmail(email, token, locale, username);

        logger.info("Outgoing response after verifying an email: {}. Status Code: {}. Message: {}", userResponse,
                userResponse.getStatusCode(), userResponse.getMessage());

        return userResponse;
    }

    @Override
    public UserResponse resendVerificationLink(String email, Locale locale, String username) {

        logger.info("Incoming request to resend verification link to email: {}", email);

        UserResponse userResponse = userService.resendVerificationLink(email, locale, username);

        logger.info("Outgoing response after resending verification link: {}. Status Code: {}. Message: {}",
                userResponse, userResponse.getStatusCode(), userResponse.getMessage());

        return userResponse;
    }

    @Override
    public UserResponse forgotPassword(ForgotPasswordRequest forgotPasswordRequest, Locale locale) {

        logger.info("Incoming request for forgot password: {}", forgotPasswordRequest);

        UserResponse userResponse = userService.forgotPassword(forgotPasswordRequest, locale);

        logger.info("Outgoing response for forgot password: {}. Status Code: {}. Message: {}",
                userResponse, userResponse.getStatusCode(), userResponse.getMessage());

        return userResponse;
    }

    @Override
    public UserResponse validateResetToken(String token, String email, Locale locale) {

        logger.info("Incoming request to validate reset token for email: {}", email);

        UserResponse userResponse = userService.validateResetToken(token, email, locale);

        logger.info("Outgoing response for validate reset token: {}. Status Code: {}. Message: {}",
                userResponse, userResponse.getStatusCode(), userResponse.getMessage());

        return userResponse;
    }
    
    @Override
    public UserResponse findByPhoneNumberOrEmail(String phoneNumberOrEmail, Locale locale) {

        logger.info("Incoming request to find a user by phone number or email: {}", phoneNumberOrEmail);

        UserResponse userResponse = userService.findByPhoneNumberOrEmail(phoneNumberOrEmail, locale);

        logger.info("Outgoing response after finding a user by phone number or email: {}. Status Code: {}. Message: {}",
                userResponse, userResponse.getStatusCode(), userResponse.getMessage());

        return userResponse;
    }
}
