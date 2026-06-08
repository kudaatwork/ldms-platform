package projectlx.user.management.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Stores BCrypt-hashed one-time passcodes for phone verification, login 2FA, and step-up auth.
 * OTPs are 6 digits, expire in 10 minutes, and are single-use (used=true after consumption).
 */
@Entity
@Table(
        name = "user_otp_challenge",
        indexes = {
                @Index(name = "idx_otp_user_id",        columnList = "user_id"),
                @Index(name = "idx_otp_user_type_used", columnList = "user_id, otp_type, used")
        }
)
@Getter
@Setter
@ToString
public class UserOtpChallenge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // =============================
    //  Ownership & Purpose
    // =============================

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "otp_type", nullable = false, length = 50)
    private OtpType otpType;

    // =============================
    //  OTP State
    // =============================

    /** BCrypt hash of the 6-digit OTP — never store plain-text. */
    @Column(name = "otp_hash", nullable = false, length = 255)
    private String otpHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used", nullable = false)
    private Boolean used = false;

    // =============================
    //  Soft Delete / Audit
    // =============================

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_status", nullable = false, length = 50)
    private EntityStatus entityStatus = EntityStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;
}
