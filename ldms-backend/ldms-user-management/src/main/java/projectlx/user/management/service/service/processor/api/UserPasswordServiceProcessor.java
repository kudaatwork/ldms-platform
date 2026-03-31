package projectlx.user.management.service.service.processor.api;

import projectlx.user.management.service.utils.requests.ChangeUserPasswordRequest;
import projectlx.user.management.service.utils.requests.ResetPasswordRequest;
import projectlx.user.management.service.utils.responses.ExpiringPasswordsResponse;
import projectlx.user.management.service.utils.responses.UserPasswordResponse;
import java.time.LocalDateTime;
import java.util.Locale;

public interface UserPasswordServiceProcessor {
    UserPasswordResponse update(ChangeUserPasswordRequest changeUserPasswordRequest, String username, Locale locale);
    UserPasswordResponse resetPassword(ResetPasswordRequest resetPasswordRequest, Locale locale, String username);
}
