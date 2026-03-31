package projectlx.user.authentication.service.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum TokenType {

    BEARER("Bearer");

    private final String tokenType;
}
