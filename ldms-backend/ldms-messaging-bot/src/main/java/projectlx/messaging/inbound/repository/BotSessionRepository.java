package projectlx.messaging.inbound.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.messaging.inbound.model.BotSession;

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
}
