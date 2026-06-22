package projectlx.messaging.inbound.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.messaging.inbound.model.BotKnowledgeDocument;

import java.util.List;
import java.util.Optional;

public interface BotKnowledgeDocumentRepository extends JpaRepository<BotKnowledgeDocument, Long> {

    Optional<BotKnowledgeDocument> findByIdAndEntityStatusNot(Long id, EntityStatus excluded);

    List<BotKnowledgeDocument> findByEntityStatusNotOrderByModifiedAtDescCreatedAtDesc(EntityStatus excluded);

    List<BotKnowledgeDocument> findByPublishedTrueAndEntityStatusNotOrderByModifiedAtDescCreatedAtDesc(EntityStatus excluded);

    @Modifying
    @Query("UPDATE BotKnowledgeDocument d SET d.useCount = d.useCount + 1 WHERE d.id IN :ids")
    void incrementUseCount(@Param("ids") List<Long> ids);
}
