package projectlx.user.authentication.service.business.auditable.impl;

import lombok.RequiredArgsConstructor;
import projectlx.user.authentication.service.business.auditable.api.AuthenticationServiceAuditable;
import projectlx.user.authentication.service.model.Token;
import projectlx.user.authentication.service.repository.TokenRepository;
import java.util.Locale;

@RequiredArgsConstructor
public class AuthenticationServiceAuditableImpl implements AuthenticationServiceAuditable {

    private final TokenRepository tokenRepository;

    @Override
    public Token create(Token token, Locale locale, String username) {
        return tokenRepository.save(token);
    }
}
