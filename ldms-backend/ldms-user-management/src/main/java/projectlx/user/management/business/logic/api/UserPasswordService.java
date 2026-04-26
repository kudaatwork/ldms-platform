package projectlx.user.management.business.logic.api;

import projectlx.user.management.utils.requests.ChangeUserPasswordRequest;
import projectlx.user.management.utils.requests.CreateUserPasswordRequest;
import projectlx.user.management.utils.requests.ResetPasswordRequest;
import projectlx.user.management.utils.responses.ExpiringPasswordsResponse;
import projectlx.user.management.utils.responses.UserPasswordResponse;
import java.time.LocalDateTime;
import java.util.Locale;

public interface UserPasswordService {
    UserPasswordResponse create(CreateUserPasswordRequest createUserPasswordRequest, Locale locale, String username);
    UserPasswordResponse findById(Long id, Locale locale);
    UserPasswordResponse findAllAsList(String username, Locale locale);
    UserPasswordResponse update(ChangeUserPasswordRequest changeUserPasswordRequest, String username, Locale locale);
    UserPasswordResponse delete(Long id, Locale locale);
    UserPasswordResponse resetPassword(ResetPasswordRequest resetPasswordRequest, Locale locale, String username);
    
    /**
     * Find passwords that are about to expire within the specified date range
     * @param startDate The start date of the range (inclusive)
     * @param endDate The end date of the range (inclusive)
     * @return List of passwords that are about to expire with their associated users
     */
    ExpiringPasswordsResponse findPasswordsAboutToExpire(LocalDateTime startDate, LocalDateTime endDate);
}
