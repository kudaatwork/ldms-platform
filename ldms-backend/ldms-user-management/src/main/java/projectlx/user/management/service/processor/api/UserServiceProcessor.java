package projectlx.user.management.service.processor.api;

import com.lowagie.text.DocumentException;
import projectlx.user.management.utils.dtos.ImportSummary;
import projectlx.user.management.utils.requests.CreateUserRequest;
import projectlx.user.management.utils.requests.EditUserRequest;
import projectlx.user.management.utils.requests.ForgotPasswordRequest;
import projectlx.user.management.utils.requests.UsersMultipleFiltersRequest;
import projectlx.user.management.utils.responses.UserResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

public interface UserServiceProcessor {
    UserResponse create(CreateUserRequest createUserRequest, Locale locale, String username);
    UserResponse findById(Long id, Locale locale, String username);
    UserResponse findByUsername(String username, Locale locale);
    UserResponse findAllAsList(Locale locale, String username);
    UserResponse update(EditUserRequest editUserRequest, String username, Locale locale);
    UserResponse delete(Long id, Locale locale, String username);
    UserResponse findByMultipleFilters(UsersMultipleFiltersRequest usersMultipleFiltersRequest, String username,
                                       Locale locale);
    UserResponse findByOrganizationId(Long organizationId, Locale locale, String username);
    UserResponse findByBranchId(Long branchId, Locale locale, String username);
    byte[] exportToCsv(UsersMultipleFiltersRequest filters, String username, Locale locale);
    byte[] exportToExcel(UsersMultipleFiltersRequest filters, String username, Locale locale) throws IOException;
    byte[] exportToPdf(UsersMultipleFiltersRequest filters, String username, Locale locale) throws DocumentException;
    ImportSummary importUsersFromCsv(InputStream csvInputStream) throws IOException;
    ImportSummary importUsersFromExcel(InputStream excelInputStream) throws IOException;
    UserResponse verifyEmail(String email, String token, Locale locale, String username);
    UserResponse forgotPassword(ForgotPasswordRequest forgotPasswordRequest, Locale locale);
    UserResponse validateResetToken(String token, String email, Locale locale);
    UserResponse findByPhoneNumberOrEmail(String phoneNumberOrEmail, Locale locale);
    UserResponse resendVerificationLink(String email, Locale locale, String username);

}
