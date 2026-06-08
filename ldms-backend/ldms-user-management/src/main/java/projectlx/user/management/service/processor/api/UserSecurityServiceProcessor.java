package projectlx.user.management.service.processor.api;

import projectlx.user.management.utils.requests.CreateUserSecurityRequest;
import projectlx.user.management.utils.requests.EditUserSecurityRequest;
import projectlx.user.management.utils.requests.TwoFactorOtpRequest;
import projectlx.user.management.utils.requests.UserSecurityMultipleFiltersRequest;
import projectlx.user.management.utils.responses.UserSecurityResponse;
import java.util.Locale;

public interface UserSecurityServiceProcessor {
    UserSecurityResponse create(CreateUserSecurityRequest createUserSecurityRequest, Locale locale,
                                String username);
    UserSecurityResponse findById(Long id, Locale locale, String username);
    UserSecurityResponse findAllAsList(String username, Locale locale);
    UserSecurityResponse update(EditUserSecurityRequest editUserSecurityRequest, String username, Locale locale);
    UserSecurityResponse delete(Long id, Locale locale, String username);
    UserSecurityResponse findByMultipleFilters(UserSecurityMultipleFiltersRequest userSecurityMultipleFiltersRequest,
                                               String username, Locale locale);

    UserSecurityResponse findMySecurity(Locale locale, String sessionUsername);

    UserSecurityResponse saveMySecurity(EditUserSecurityRequest request, Locale locale, String sessionUsername);

    UserSecurityResponse beginMyAuthenticatorSetup(Locale locale, String sessionUsername);

    UserSecurityResponse confirmMyAuthenticatorSetup(TwoFactorOtpRequest request, Locale locale, String sessionUsername);

    UserSecurityResponse enableMySmsTwoFactor(Locale locale, String sessionUsername);

    UserSecurityResponse requestMyTwoFactorDisableOtp(Locale locale, String sessionUsername);

    UserSecurityResponse disableMyTwoFactor(TwoFactorOtpRequest request, Locale locale, String sessionUsername);

    UserSecurityResponse adminBeginAuthenticatorSetup(Long userId, Locale locale, String actor);

    UserSecurityResponse adminConfirmAuthenticatorSetup(Long userId, TwoFactorOtpRequest request, Locale locale,
                                                        String actor);

    UserSecurityResponse adminEnableSmsTwoFactor(Long userId, Locale locale, String actor);

    UserSecurityResponse adminDisableTwoFactor(Long userId, Locale locale, String actor);
}
