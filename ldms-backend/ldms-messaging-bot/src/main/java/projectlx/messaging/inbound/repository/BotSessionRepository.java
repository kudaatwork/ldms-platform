package projectlx.messaging.inbound.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.messaging.inbound.model.BotSession;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BotSessionRepository extends JpaRepository<BotSession, Long> {

    Optional<BotSession> findBySessionIdAndEntityStatus(String sessionId, EntityStatus entityStatus);

    List<BotSession> findByRequesterUsernameAndEntityStatusOrderByModifiedAtDescCreatedAtDesc(
            String requesterUsername, EntityStatus entityStatus);

    @Query("""
            SELECT s FROM BotSession s
            WHERE s.entityStatus = :entityStatus
            ORDER BY COALESCE(s.modifiedAt, s.createdAt) DESC
            """)
    List<BotSession> findAllActiveSessions(@Param("entityStatus") EntityStatus entityStatus);

    long countByEntityStatusNot(EntityStatus entityStatus);

    long countByEntityStatusNotAndCreatedAtGreaterThanEqual(EntityStatus entityStatus, LocalDateTime since);

    @Query("""
            SELECT AVG(s.satisfactionScore) FROM BotSession s
            WHERE s.entityStatus <> :excluded
              AND s.satisfactionScore IS NOT NULL
            """)
    Double averageSatisfactionScore(@Param("excluded") EntityStatus excluded);

    long countByEntityStatusNotAndSatisfactionScoreIsNotNull(EntityStatus entityStatus);

    @Query("""
            SELECT FUNCTION('DATE', s.createdAt), COUNT(s)
            FROM BotSession s
            WHERE s.entityStatus <> :excluded
              AND s.createdAt >= :since
            GROUP BY FUNCTION('DATE', s.createdAt)
            ORDER BY FUNCTION('DATE', s.createdAt)
            """)
    List<Object[]> countSessionsGroupedByDay(@Param("excluded") EntityStatus excluded,
                                             @Param("since") LocalDateTime since);

    @Query("""
            SELECT s.topic, COUNT(s)
            FROM BotSession s
            WHERE s.entityStatus <> :excluded
              AND s.topic IS NOT NULL
              AND TRIM(s.topic) <> ''
            GROUP BY s.topic
            ORDER BY COUNT(s) DESC
            """)
    List<Object[]> findTopTopics(@Param("excluded") EntityStatus excluded);

    @Query("""
            SELECT s.channel, COUNT(s)
            FROM BotSession s
            WHERE s.entityStatus <> :excluded
            GROUP BY s.channel
            ORDER BY COUNT(s) DESC
            """)
    List<Object[]> countByChannel(@Param("excluded") EntityStatus excluded);
}
