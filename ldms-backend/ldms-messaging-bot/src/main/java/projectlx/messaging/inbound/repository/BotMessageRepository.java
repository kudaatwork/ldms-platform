package projectlx.messaging.inbound.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.messaging.inbound.model.BotMessage;

import java.util.List;

public interface BotMessageRepository extends JpaRepository<BotMessage, Long> {

    List<BotMessage> findByBotSessionIdAndEntityStatusOrderByCreatedAtAsc(Long botSessionId, EntityStatus entityStatus);

    long countByBotSessionIdAndEntityStatus(Long botSessionId, EntityStatus entityStatus);
}
