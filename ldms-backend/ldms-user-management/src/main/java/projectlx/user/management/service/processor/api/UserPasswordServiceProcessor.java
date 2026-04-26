package projectlx.user.management.service.processor.api;

import projectlx.user.management.utils.requests.ChangeUserPasswordRequest;
import projectlx.user.management.utils.requests.ResetPasswordRequest;
import projectlx.user.management.utils.responses.UserPasswordResponse;

import java.util.Locale;

public interface UserPasswordServiceProcessor {
    UserPasswordResponse update(ChangeUserPasswordRequest changeUserPasswordRequest, String username, Locale locale);
    UserPasswordResponse resetPassword(ResetPasswordRequest resetPasswordRequest, Locale locale, String username);
}
