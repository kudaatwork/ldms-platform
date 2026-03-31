package projectlx.user.management.service.repository;

import projectlx.user.management.service.model.EntityStatus;
import projectlx.user.management.service.model.UserPreferences;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;
import java.util.Optional;

public interface UserPreferencesRepository extends JpaRepository<UserPreferences, Long>, JpaSpecificationExecutor<UserPreferences> {
    Optional<UserPreferences> findByIdAndEntityStatusNot(Long id, EntityStatus entityStatus);
    List<UserPreferences> findByEntityStatusNot(EntityStatus entityStatus);
    long countByPreferredLanguageAndTimezoneAndEntityStatusNot(String preferredLanguage, String timezone,
                                                               EntityStatus entityStatus);
}
