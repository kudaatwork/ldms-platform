package projectlx.user.authentication.service.utils.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum I18Code {

    MESSAGE_AUTHENTICATION_INVALID_REQUEST("message.authentication.invalidRequest"),
    MESSAGE_AUTHENTICATION_USER_NOT_FOUND("message.authentication.userNotFound"),
    MESSAGE_AUTHENTICATION_BAD_CREDENTIALS("message.authentication.badCredentials"),
    MESSAGE_AUTHENTICATION_ACCOUNT_DISABLED("message.authentication.accountDisabled"),
    MESSAGE_AUTHENTICATION_ACCOUNT_LOCKED("message.authentication.accountLocked"),
    MESSAGE_USER_AUTHENTICATED_SUCCESSFULLY("message.user.authenticated.successfully"),
    MESSAGE_REFRESH_TOKEN_REQUEST_INVALID("message.refresh.token.request.invalid"),
    MESSAGE_USER_NOT_FOUND("message.user.not.found"),
    MESSAGE_REFRESH_TOKEN_REFRESHED_SUCCESSFULLY("message.refresh.token.refreshed.successfully"),
    MESSAGE_OAUTH2_UNAUTHORIZED("message.oauth2.unauthorized"),
    MESSAGE_USER_NOT_FOUND_FOR_USERNAME("message.user.not.found.for.username"),
    MESSAGE_GOOGLE_LOGIN_DISABLED("message.google.login.disabled"),
    MESSAGE_GOOGLE_INVALID_TOKEN("message.google.invalid.token"),
    MESSAGE_GOOGLE_EMAIL_NOT_VERIFIED("message.google.email.not.verified"),
    MESSAGE_GOOGLE_USER_NOT_FOUND("message.google.user.not.found"),
    MESSAGE_GOOGLE_NOT_CONFIGURED("message.google.not.configured"),

    // Two-factor authentication
    MESSAGE_TWO_FACTOR_REQUIRED("message.two.factor.required"),
    MESSAGE_TWO_FACTOR_REQUIRED_SMS("message.two.factor.required.sms"),
    MESSAGE_TWO_FACTOR_REQUIRED_AUTHENTICATOR("message.two.factor.required.authenticator"),
    MESSAGE_TWO_FACTOR_CHALLENGE_INVALID("message.two.factor.challenge.invalid"),
    MESSAGE_TWO_FACTOR_OTP_INVALID("message.two.factor.otp.invalid"),
    MESSAGE_TWO_FACTOR_VERIFIED_SUCCESSFULLY("message.two.factor.verified.successfully"),
    MESSAGE_TWO_FACTOR_REQUEST_INVALID("message.two.factor.request.invalid"),
    MESSAGE_USER_MANAGEMENT_UNAVAILABLE("message.user.management.unavailable");

    private final String code;
}
