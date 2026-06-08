package projectlx.user.management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import projectlx.user.management.model.EntityStatus;
import projectlx.user.management.model.OtpType;
import projectlx.user.management.model.UserOtpChallenge;

import java.time.LocalDateTime;
import java.util.Optional;

public interface UserOtpChallengeRepository extends JpaRepository<UserOtpChallenge, Long> {

    /**
     * Finds the most recent unused, unexpired, active OTP challenge for a user and type.
     */
    Optional<UserOtpChallenge> findTopByUserIdAndOtpTypeAndUsedFalseAndExpiresAtAfterAndEntityStatusNotOrderByCreatedAtDesc(
            Long userId, OtpType otpType, LocalDateTime now, EntityStatus entityStatus);

    /**
     * Marks all previous unused challenges for a user/type as used before issuing a fresh one.
     * Prevents accumulation of stale challenges.
     */
    @Modifying
    @Query("UPDATE UserOtpChallenge c SET c.used = true " +
           "WHERE c.userId = :userId AND c.otpType = :otpType AND c.used = false")
    void invalidatePreviousChallenges(@Param("userId") Long userId, @Param("otpType") OtpType otpType);
}
