package projectlx.messaging.inbound.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.messaging.inbound.model.BotFaq;

import java.util.List;
import java.util.Optional;

public interface BotFaqRepository extends JpaRepository<BotFaq, Long> {

    Optional<BotFaq> findByIdAndEntityStatusNot(Long id, EntityStatus excluded);

    List<BotFaq> findByEntityStatusNotOrderByModifiedAtDescCreatedAtDesc(EntityStatus excluded);

    List<BotFaq> findByPublishedTrueAndEntityStatusNotOrderByModifiedAtDescCreatedAtDesc(EntityStatus excluded);

    long countByPublishedTrueAndEntityStatusNot(EntityStatus excluded);

    @Modifying
    @Query("UPDATE BotFaq f SET f.useCount = f.useCount + 1 WHERE f.id IN :ids")
    void incrementUseCount(@Param("ids") List<Long> ids);
}
