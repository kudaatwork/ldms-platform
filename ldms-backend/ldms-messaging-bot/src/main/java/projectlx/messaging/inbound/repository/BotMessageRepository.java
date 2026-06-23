package projectlx.messaging.inbound.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.messaging.inbound.model.BotMessage;
import projectlx.messaging.inbound.repository.projection.BotMessageDailyCountProjection;
import projectlx.messaging.inbound.utils.enums.BotMessageRole;

import java.time.LocalDateTime;
import java.util.List;

public interface BotMessageRepository extends JpaRepository<BotMessage, Long> {

    List<BotMessage> findByBotSessionIdAndEntityStatusOrderByCreatedAtAsc(Long botSessionId, EntityStatus entityStatus);

    long countByBotSessionIdAndEntityStatus(Long botSessionId, EntityStatus entityStatus);

    long countByEntityStatusNot(EntityStatus entityStatus);

    long countByRoleAndEntityStatusNot(BotMessageRole role, EntityStatus entityStatus);

    @Query(value = """
            SELECT DATE(m.created_at) AS day,
                   COALESCE(SUM(CASE WHEN m.role = 'USER' THEN 1 ELSE 0 END), 0) AS userMessages,
                   COALESCE(SUM(CASE WHEN m.role = 'BOT' THEN 1 ELSE 0 END), 0) AS botMessages
            FROM bot_message m
            WHERE m.entity_status <> 'DELETED'
              AND m.created_at >= :since
            GROUP BY DATE(m.created_at)
            ORDER BY day
            """, nativeQuery = true)
    List<BotMessageDailyCountProjection> countMessagesGroupedByDay(@Param("since") LocalDateTime since);

    @Query("""
            SELECT COUNT(m) FROM BotMessage m
            WHERE m.entityStatus <> :excluded
              AND m.createdAt >= :since
            """)
    long countSince(@Param("excluded") EntityStatus excluded, @Param("since") LocalDateTime since);
}
