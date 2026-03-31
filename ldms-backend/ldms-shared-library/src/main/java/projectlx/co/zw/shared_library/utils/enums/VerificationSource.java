package projectlx.co.zw.shared_library.utils.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum VerificationSource {

    ADMIN_PANEL("ADMIN_PANEL"),   // Verified through the platform's admin interface
    GOVERNMENT_API("GOVERNMENT_API"), // Verified through official government systems
    AZURE_COGNITIVE_SERVICES("AZURE_COGNITIVE_SERVICES"), // Verified through third-party Microsoft Azure Cognitive Services
    SYSTEM_AUTOMATION("SYSTEM_AUTOMATION");  // Automatically verified by system processes

    private final String verificationSource;
}
