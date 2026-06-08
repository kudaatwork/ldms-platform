package projectlx.user.management.model;

/**
 * Discriminates the purpose of a {@link UserOtpChallenge} record.
 * Stored as VARCHAR(50) — never use MySQL ENUM type.
 */
public enum OtpType {

    /** Verifying that the user owns the registered phone number. */
    PHONE_VERIFICATION,

    /** Two-factor authentication challenge at login. */
    LOGIN_2FA,

    /** One-time step-up authentication for sensitive portal actions. */
    STEP_UP
}
