package projectlx.co.zw.shared_library.utils.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum VerificationMethod {

    MANUAL("MANUAL"),             // Verified manually by an admin or human agent
    AUTOMATED("AUTOMATED"),       // Verified automatically by internal algorithms
    EXTERNAL_API("EXTERNAL_API"); // Verified via integration with external verification APIs or services

    private final String verificationMethod;
}
