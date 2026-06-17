package projectlx.user.management.service.processor.impl;

import com.lowagie.text.DocumentException;
import org.springframework.data.domain.Page;
import projectlx.user.management.business.logic.api.UserService;
import projectlx.user.management.business.logic.support.OrganizationContactCredentialsIssuer;
import projectlx.user.management.business.logic.support.OrganizationContactPersonProvisioner;
import projectlx.user.management.service.processor.api.UserServiceProcessor;
import projectlx.user.management.utils.dtos.ImportSummary;
import projectlx.user.management.utils.dtos.UserDto;
import projectlx.user.management.utils.requests.CreateUserRequest;
import projectlx.user.management.utils.requests.EditUserRequest;
import projectlx.user.management.utils.requests.ForgotPasswordRequest;
import projectlx.user.management.utils.requests.CompleteCredentialsSetupRequest;
import projectlx.user.management.utils.requests.IssueOrganizationContactCredentialsRequest;
import projectlx.user.management.utils.requests.ProvisionOrganizationContactPersonRequest;
import projectlx.user.management.utils.requests.UsersMultipleFiltersRequest;
import projectlx.user.management.utils.responses.UsernameAvailabilityResponse;
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
    private final OrganizationContactPersonProvisioner organizationContactPersonProvisioner;
    private final OrganizationContactCredentialsIssuer organizationContactCredentialsIssuer;
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
    public UserResponse findCurrentUserForSession(String username, Locale locale) {

        logger.info("Incoming request for current user session profile: {}", username);

        UserResponse userResponse = userService.findCurrentUserForSession(username, locale);

        logger.info("Outgoing response for current user session profile. Status Code: {}. Message: {}",
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
    public UserResponse findUsernamesByOrganizationId(Long organizationId, Locale locale, String username) {

        logger.info("Incoming request to find usernames by organization id: {}", organizationId);

        UserResponse userResponse = userService.findUsernamesByOrganizationId(organizationId, locale, username);

        logger.info(
                "Outgoing response after finding usernames by organization id: {}. Status Code: {}. Message: {}",
                userResponse,
                userResponse.getStatusCode(),
                userResponse.getMessage());

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
    public UserResponse forgotPassword(ForgotPasswordRequest forgotPasswordRequest, String clientPlatform,
            Locale locale) {

        logger.info("Incoming request for forgot password: {}", forgotPasswordRequest);

        UserResponse userResponse = userService.forgotPassword(forgotPasswordRequest, clientPlatform, locale);

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
    public UserResponse setOrganizationKycApprover(Long id, boolean enabled, Locale locale, String username) {
        logger.info("Incoming request to set organisation KYC approver for user {}: {}", id, enabled);
        UserResponse userResponse = userService.setOrganizationKycApprover(id, enabled, locale, username);
        logger.info("Outgoing response after setting organisation KYC approver. Status Code: {}. Message: {}",
                userResponse.getStatusCode(), userResponse.getMessage());
        return userResponse;
    }

    @Override
    public UserResponse listOrganizationKycApprovers(Locale locale) {
        logger.info("Incoming request to list organisation KYC approvers");
        UserResponse userResponse = userService.listOrganizationKycApprovers(locale);
        logger.info("Outgoing response after listing organisation KYC approvers. Status Code: {}. Message: {}",
                userResponse.getStatusCode(), userResponse.getMessage());
        return userResponse;
    }

    @Override
    public UserResponse listOperationalIssueHandlers(Locale locale) {
        logger.info("Incoming request to list operational issue handlers");
        UserResponse userResponse = userService.listOperationalIssueHandlers(locale);
        logger.info("Outgoing response after listing operational issue handlers. Status Code: {}. Message: {}",
                userResponse.getStatusCode(), userResponse.getMessage());
        return userResponse;
    }

    @Override
    public UserResponse setOperationalIssueHandler(Long id, boolean enabled, Locale locale, String username) {
        logger.info("Incoming request to set operational issue handler for user {}: {}", id, enabled);
        UserResponse userResponse = userService.setOperationalIssueHandler(id, enabled, locale, username);
        logger.info("Outgoing response after setting operational issue handler. Status Code: {}. Message: {}",
                userResponse.getStatusCode(), userResponse.getMessage());
        return userResponse;
    }

    @Override
    public UserResponse listProcurementApprovers(Locale locale, String username) {
        logger.info("Incoming request to list procurement approvers");
        UserResponse userResponse = userService.listProcurementApprovers(locale, username);
        logger.info("Outgoing response after listing procurement approvers. Status Code: {}. Message: {}",
                userResponse.getStatusCode(), userResponse.getMessage());
        return userResponse;
    }

    @Override
    public UserResponse setProcurementApprover(Long id, boolean enabled, Locale locale, String username) {
        logger.info("Incoming request to set procurement approver for user {}: {}", id, enabled);
        UserResponse userResponse = userService.setProcurementApprover(id, enabled, locale, username);
        logger.info("Outgoing response after setting procurement approver. Status Code: {}. Message: {}",
                userResponse.getStatusCode(), userResponse.getMessage());
        return userResponse;
    }

    @Override
    public UserResponse setShipmentFleetAllocator(Long id, boolean enabled, Locale locale, String username) {
        logger.info("Incoming request to set shipment fleet allocator for user {}: {}", id, enabled);
        UserResponse userResponse = userService.setShipmentFleetAllocator(id, enabled, locale, username);
        logger.info("Outgoing response after setting shipment fleet allocator. Status Code: {}. Message: {}",
                userResponse.getStatusCode(), userResponse.getMessage());
        return userResponse;
    }

    @Override
    public UserResponse findFleetManagersByOrganization(Long organizationId, Locale locale) {
        logger.info("Incoming request to find fleet managers for organizationId={}", organizationId);
        UserResponse userResponse = userService.findFleetManagersByOrganization(organizationId, locale);
        logger.info("Outgoing response after finding fleet managers. Status Code: {}. Message: {}",
                userResponse.getStatusCode(), userResponse.getMessage());
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

    @Override
    public UserResponse provisionOrganizationContactPerson(ProvisionOrganizationContactPersonRequest request,
            Locale locale, String username) {
        logger.info("Incoming request to provision organisation contact person for organisation {}",
                request != null ? request.getOrganizationId() : null);
        UserResponse userResponse = organizationContactPersonProvisioner.provision(request, locale, username);
        if (userResponse != null) {
            logger.info(
                    "Outgoing response after provisioning organisation contact person. Status Code: {}. Message: {}",
                    userResponse.getStatusCode(),
                    userResponse.getMessage());
        }
        return userResponse;
    }

    @Override
    public UserResponse issueOrganizationContactCredentials(IssueOrganizationContactCredentialsRequest request,
            Locale locale, String username) {
        logger.info("Incoming request to issue organisation contact credentials for organisation {}",
                request != null ? request.getOrganizationId() : null);
        UserResponse userResponse =
                organizationContactCredentialsIssuer.issueTemporaryCredentials(request, locale, username);
        if (userResponse != null) {
            logger.info(
                    "Outgoing response after issuing organisation contact credentials. Status Code: {}. Message: {}",
                    userResponse.getStatusCode(),
                    userResponse.getMessage());
        }
        return userResponse;
    }

    @Override
    public UserResponse completeCredentialsSetup(CompleteCredentialsSetupRequest request, Locale locale,
            String username) {
        logger.info("Incoming request to complete credential setup for {}", username);
        UserResponse userResponse =
                organizationContactCredentialsIssuer.completeCredentialsSetup(request, username, locale, username);
        if (userResponse != null) {
            logger.info(
                    "Outgoing response after completing credential setup. Status Code: {}. Message: {}",
                    userResponse.getStatusCode(),
                    userResponse.getMessage());
        }
        return userResponse;
    }

    @Override
    public UsernameAvailabilityResponse checkUsernameAvailability(String candidateUsername, Locale locale,
            String username) {
        logger.info("Incoming request to check username availability for {}", username);
        return organizationContactCredentialsIssuer.checkUsernameAvailability(candidateUsername, username, locale);
    }

    // ============================================================
    //  Phone verification & step-up
    // ============================================================

    @Override
    public UserResponse requestPhoneVerification(String username, Locale locale) {
        logger.info("Incoming request to initiate phone verification for user: {}", username);
        UserResponse response = userService.requestPhoneVerification(username, locale);
        logger.info("Phone verification OTP result for {}: status={}", username, response.getStatusCode());
        return response;
    }

    @Override
    public UserResponse confirmPhoneVerification(String username, String otp, Locale locale) {
        logger.info("Incoming request to confirm phone verification for user: {}", username);
        UserResponse response = userService.confirmPhoneVerification(username, otp, locale);
        logger.info("Phone verification confirmation result for {}: status={}", username, response.getStatusCode());
        return response;
    }

    @Override
    public UserResponse requestStepUpVerification(String username, Locale locale) {
        logger.info("Incoming request to initiate step-up OTP for user: {}", username);
        UserResponse response = userService.requestStepUpVerification(username, locale);
        logger.info("Step-up OTP result for {}: status={}", username, response.getStatusCode());
        return response;
    }

    @Override
    public UserResponse confirmStepUpVerification(String username, String otp, Locale locale) {
        logger.info("Incoming request to confirm step-up OTP for user: {}", username);
        UserResponse response = userService.confirmStepUpVerification(username, otp, locale);
        logger.info("Step-up OTP confirmation result for {}: status={}", username, response.getStatusCode());
        return response;
    }

    // ============================================================
    //  System-only login OTP
    // ============================================================

    @Override
    public UserResponse generateLoginOtp(String usernameOrPhone, Locale locale) {
        logger.info("Incoming system request to generate login OTP for: {}", usernameOrPhone);
        return userService.generateLoginOtp(usernameOrPhone, locale);
    }

    @Override
    public UserResponse verifyLoginOtp(String usernameOrPhone, String otp, Locale locale) {
        logger.info("Incoming system request to verify login OTP for: {}", usernameOrPhone);
        return userService.verifyLoginOtp(usernameOrPhone, otp, locale);
    }
}
