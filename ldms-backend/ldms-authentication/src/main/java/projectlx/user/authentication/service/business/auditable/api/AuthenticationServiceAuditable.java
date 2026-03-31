package projectlx.user.authentication.service.business.auditable.api;

import projectlx.user.authentication.service.model.Token;

import java.util.Locale;

public interface AuthenticationServiceAuditable {
    Token create(Token token, Locale locale, String username);
}
