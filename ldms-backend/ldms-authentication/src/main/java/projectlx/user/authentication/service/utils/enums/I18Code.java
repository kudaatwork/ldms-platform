package projectlx.user.authentication.service.utils.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum I18Code {

    MESSAGE_AUTHENTICATION_INVALID_REQUEST("message.authentication.invalidRequest"),
    MESSAGE_USER_AUTHENTICATED_SUCCESSFULLY("message.user.authenticated.successfully"),
    MESSAGE_REFRESH_TOKEN_REQUEST_INVALID("message.refresh.token.request.invalid"),
    MESSAGE_USER_NOT_FOUND("message.user.not.found"),
    MESSAGE_REFRESH_TOKEN_REFRESHED_SUCCESSFULLY("message.refresh.token.refreshed.successfully");

    private final String code;
}
