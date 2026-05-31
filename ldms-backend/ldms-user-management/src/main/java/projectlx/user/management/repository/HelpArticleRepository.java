package projectlx.user.management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import projectlx.user.management.model.EntityStatus;
import projectlx.user.management.model.HelpArticle;
import projectlx.user.management.model.HelpArticleCategory;

import java.util.List;
import java.util.Optional;

public interface HelpArticleRepository extends JpaRepository<HelpArticle, Long> {

    List<HelpArticle> findByEntityStatusOrderBySortOrderAscTitleAsc(EntityStatus entityStatus);

    List<HelpArticle> findByCategoryAndEntityStatusOrderBySortOrderAscTitleAsc(
            HelpArticleCategory category, EntityStatus entityStatus);

    Optional<HelpArticle> findBySlugAndEntityStatus(String slug, EntityStatus entityStatus);
}
