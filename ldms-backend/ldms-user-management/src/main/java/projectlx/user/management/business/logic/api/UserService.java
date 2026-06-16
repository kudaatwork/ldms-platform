package projectlx.user.management.business.logic.api;

import com.lowagie.text.DocumentException;
import projectlx.user.management.utils.dtos.ImportSummary;
import projectlx.user.management.utils.dtos.UserDto;
import projectlx.user.management.utils.requests.CreateUserRequest;
import projectlx.user.management.utils.requests.EditUserRequest;
import projectlx.user.management.utils.requests.ForgotPasswordRequest;
import projectlx.user.management.utils.requests.UsersMultipleFiltersRequest;
import projectlx.user.management.utils.responses.UserResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

public interface UserService {
    UserResponse create(CreateUserRequest createUserRequest, Locale locale, String username);
    UserResponse findById(Long id, Locale locale, String username);
    UserResponse findByUsername(String username, Locale locale);

    /** Lightweight profile for the signed-in session (skips remote address hydration). */
    UserResponse findCurrentUserForSession(String username, Locale locale);
    UserResponse findAllAsList(Locale locale, String username);
    UserResponse update(EditUserRequest editUserRequest, String username, Locale locale);
    UserResponse delete(Long id, Locale locale, String username);
    UserResponse findByMultipleFilters(UsersMultipleFiltersRequest usersMultipleFiltersRequest, String username,
                                       Locale locale);
    UserResponse findByOrganizationId(Long organizationId, Locale locale, String username);

    /** Distinct login usernames for an organisation workspace (audit scoping). */
    UserResponse findUsernamesByOrganizationId(Long organizationId, Locale locale, String username);

    UserResponse findByBranchId(Long branchId, Locale locale, String username);
    byte[] exportToCsv(List<UserDto> users);
    byte[] exportToExcel(List<UserDto> users) throws IOException;
    byte[] exportToPdf(List<UserDto> users) throws DocumentException;
    ImportSummary importUsersFromCsv(InputStream csvInputStream) throws IOException;
    ImportSummary importUsersFromExcel(InputStream excelInputStream) throws IOException;
    UserResponse verifyEmail(String email, String token, Locale locale, String username);
    UserResponse forgotPassword(ForgotPasswordRequest forgotPasswordRequest, String clientPlatform, Locale locale);
    UserResponse validateResetToken(String token, String email, Locale locale);
    UserResponse findByPhoneNumberOrEmail(String phoneNumberOrEmail, Locale locale);
    UserResponse resendVerificationLink(String email, Locale locale, String username);

    /** Admin-portal users flagged as organisation KYC approvers (no organisation assignment). */
    UserResponse listOrganizationKycApprovers(Locale locale);

    UserResponse setOrganizationKycApprover(Long id, boolean enabled, Locale locale, String username);

    /** Admin-portal users flagged as operational issue / support ticket handlers (no organisation assignment). */
    UserResponse listOperationalIssueHandlers(Locale locale);

    UserResponse setOperationalIssueHandler(Long id, boolean enabled, Locale locale, String username);

    /** Organisation workspace users flagged as procurement approvers for the caller's organisation. */
    UserResponse listProcurementApprovers(Locale locale, String username);

    /**
     * Returns users in the organisation workspace who hold at least one fleet-manager role
     * (ALLOCATE_SHIPMENT, VIEW_FLEET_DRIVERS, ORGANIZATION_ADMINISTRATOR).
     * System-caller endpoint — no session-org check.
     */
    UserResponse findFleetManagersByOrganization(Long organizationId, Locale locale);

    UserResponse setProcurementApprover(Long id, boolean enabled, Locale locale, String username);

    // ============================================================
    //  Phone verification & 2FA step-up
    // ============================================================

    /**
     * Generates a PHONE_VERIFICATION OTP and delivers it via SMS to the authenticated user's
     * registered phone number.  Returns a 400 if the phone is already verified.
     */
    UserResponse requestPhoneVerification(String username, Locale locale);

    /**
     * Confirms a PHONE_VERIFICATION OTP submitted by the authenticated user.
     * On success marks {@code phoneVerified=true} and stamps {@code lastPhoneVerifiedAt}.
     *
     * @param username the authenticated user
     * @param otp      the 6-digit code submitted by the user
     */
    UserResponse confirmPhoneVerification(String username, String otp, Locale locale);

    /**
     * Generates a STEP_UP OTP and delivers it via SMS.  Used before sensitive portal actions.
     */
    UserResponse requestStepUpVerification(String username, Locale locale);

    /**
     * Confirms a STEP_UP OTP.  Does NOT permanently mark the phone as verified.
     *
     * @param username the authenticated user
     * @param otp      the 6-digit code submitted by the user
     */
    UserResponse confirmStepUpVerification(String username, String otp, Locale locale);

    /**
     * System-only: generates a LOGIN_2FA OTP and delivers it via SMS.
     * Called by ldms-authentication after successful password validation.
     *
     * @param usernameOrPhone the login identifier of the user
     */
    UserResponse generateLoginOtp(String usernameOrPhone, Locale locale);

    /**
     * System-only: verifies a LOGIN_2FA OTP.
     * Called by ldms-authentication during the 2FA challenge step.
     *
     * @param usernameOrPhone the login identifier of the user
     * @param otp             the 6-digit code submitted by the user
     */
    UserResponse verifyLoginOtp(String usernameOrPhone, String otp, Locale locale);
}
