package projectlx.messaging.inbound.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.messaging.inbound.model.BotMessage;
import projectlx.messaging.inbound.utils.enums.BotMessageRole;

import java.time.LocalDateTime;
import java.util.List;

public interface BotMessageRepository extends JpaRepository<BotMessage, Long> {

    List<BotMessage> findByBotSessionIdAndEntityStatusOrderByCreatedAtAsc(Long botSessionId, EntityStatus entityStatus);

    long countByBotSessionIdAndEntityStatus(Long botSessionId, EntityStatus entityStatus);

    long countByEntityStatusNot(EntityStatus entityStatus);

    long countByRoleAndEntityStatusNot(BotMessageRole role, EntityStatus entityStatus);

    @Query("""
            SELECT COUNT(m) FROM BotMessage m
            WHERE m.entityStatus <> :excluded
              AND m.createdAt >= :since
            """)
    long countSince(@Param("excluded") EntityStatus excluded, @Param("since") LocalDateTime since);
}
