package projectlx.co.zw.shared_library.utils.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum VerificationTargetType {

    ORGANIZATION("ORGANIZATION"),
    BRANCH("BRANCH"),
    AGENT("AGENT");

    private final String verificationTargetType;
}
