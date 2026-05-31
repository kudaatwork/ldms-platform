package projectlx.user.management.utils.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Cross-cutting portal roles (admin UI guards, KYC reviewer shortcuts). Stored in {@code user_role}
 * like service-scoped roles and included in JWT {@code roles} claims.
 */
@RequiredArgsConstructor
@Getter
public enum PlatformRoles {

    ADMIN("ADMIN", "Full platform administrator (admin portal)"),
    KYC_STAGE1("KYC_STAGE1", "Organisation KYC stage-1 reviewer (admin portal)"),
    KYC_STAGE2("KYC_STAGE2", "Organisation KYC stage-2 reviewer (admin portal)"),
    KYC_STAGE3("KYC_STAGE3", "Organisation KYC stage-3 reviewer (admin portal)"),
    KYC_STAGE4("KYC_STAGE4", "Organisation KYC stage-4 reviewer (admin portal)"),
    KYC_STAGE5("KYC_STAGE5", "Organisation KYC stage-5 reviewer (admin portal)"),
    READ_ONLY("READ_ONLY", "Read-only platform access (admin portal)");

    private final String roleName;
    private final String description;
}
