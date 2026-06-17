package projectlx.user.management.service.processor.api;

import com.lowagie.text.DocumentException;
import projectlx.user.management.utils.dtos.ImportSummary;
import projectlx.user.management.utils.requests.CreateUserRequest;
import projectlx.user.management.utils.requests.EditUserRequest;
import projectlx.user.management.utils.requests.ForgotPasswordRequest;
import projectlx.user.management.utils.requests.CompleteCredentialsSetupRequest;
import projectlx.user.management.utils.requests.IssueOrganizationContactCredentialsRequest;
import projectlx.user.management.utils.requests.ProvisionOrganizationContactPersonRequest;
import projectlx.user.management.utils.requests.UsersMultipleFiltersRequest;
import projectlx.user.management.utils.responses.UsernameAvailabilityResponse;
import projectlx.user.management.utils.responses.UserResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

public interface UserServiceProcessor {
    UserResponse create(CreateUserRequest createUserRequest, Locale locale, String username);
    UserResponse findById(Long id, Locale locale, String username);
    UserResponse findByUsername(String username, Locale locale);

    UserResponse findCurrentUserForSession(String username, Locale locale);
    UserResponse findAllAsList(Locale locale, String username);
    UserResponse update(EditUserRequest editUserRequest, String username, Locale locale);
    UserResponse delete(Long id, Locale locale, String username);
    UserResponse findByMultipleFilters(UsersMultipleFiltersRequest usersMultipleFiltersRequest, String username,
                                       Locale locale);
    UserResponse findByOrganizationId(Long organizationId, Locale locale, String username);
    UserResponse findUsernamesByOrganizationId(Long organizationId, Locale locale, String username);
    UserResponse findByBranchId(Long branchId, Locale locale, String username);
    byte[] exportToCsv(UsersMultipleFiltersRequest filters, String username, Locale locale);
    byte[] exportToExcel(UsersMultipleFiltersRequest filters, String username, Locale locale) throws IOException;
    byte[] exportToPdf(UsersMultipleFiltersRequest filters, String username, Locale locale) throws DocumentException;
    ImportSummary importUsersFromCsv(InputStream csvInputStream) throws IOException;
    ImportSummary importUsersFromExcel(InputStream excelInputStream) throws IOException;
    UserResponse verifyEmail(String email, String token, Locale locale, String username);
    UserResponse forgotPassword(ForgotPasswordRequest forgotPasswordRequest, String clientPlatform, Locale locale);
    UserResponse validateResetToken(String token, String email, Locale locale);
    UserResponse findByPhoneNumberOrEmail(String phoneNumberOrEmail, Locale locale);

    UserResponse listOrganizationKycApprovers(Locale locale);

    UserResponse setOrganizationKycApprover(Long id, boolean enabled, Locale locale, String username);

    UserResponse listOperationalIssueHandlers(Locale locale);

    UserResponse setOperationalIssueHandler(Long id, boolean enabled, Locale locale, String username);

    UserResponse listProcurementApprovers(Locale locale, String username);

    UserResponse setProcurementApprover(Long id, boolean enabled, Locale locale, String username);

    UserResponse setShipmentFleetAllocator(Long id, boolean enabled, Locale locale, String username);

    /**
     * Returns fleet managers (users with ALLOCATE_SHIPMENT / VIEW_FLEET_DRIVERS /
     * ORGANIZATION_ADMINISTRATOR roles) in the given organisation workspace.
     * System-caller only — no session-org check.
     */
    UserResponse findFleetManagersByOrganization(Long organizationId, Locale locale);

    UserResponse resendVerificationLink(String email, Locale locale, String username);

    UserResponse provisionOrganizationContactPerson(ProvisionOrganizationContactPersonRequest request, Locale locale,
            String username);

    UserResponse issueOrganizationContactCredentials(IssueOrganizationContactCredentialsRequest request, Locale locale,
            String username);

    UserResponse completeCredentialsSetup(CompleteCredentialsSetupRequest request, Locale locale, String username);

    UsernameAvailabilityResponse checkUsernameAvailability(String candidateUsername, Locale locale, String username);

    // ============================================================
    //  Phone verification & step-up
    // ============================================================

    UserResponse requestPhoneVerification(String username, Locale locale);

    UserResponse confirmPhoneVerification(String username, String otp, Locale locale);

    UserResponse requestStepUpVerification(String username, Locale locale);

    UserResponse confirmStepUpVerification(String username, String otp, Locale locale);

    // ============================================================
    //  System-only login OTP (called by ldms-authentication)
    // ============================================================

    UserResponse generateLoginOtp(String usernameOrPhone, Locale locale);

    UserResponse verifyLoginOtp(String usernameOrPhone, String otp, Locale locale);
}
