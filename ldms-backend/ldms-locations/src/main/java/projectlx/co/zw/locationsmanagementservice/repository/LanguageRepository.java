package projectlx.co.zw.locationsmanagementservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import projectlx.co.zw.locationsmanagementservice.model.Language;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.Optional;

public interface LanguageRepository extends JpaRepository<Language, Long>, JpaSpecificationExecutor<Language> {
    Optional<Language> findByNameAndEntityStatusNot(String name, EntityStatus entityStatus);
    Optional<Language> findByIdAndEntityStatusNot(Long id, EntityStatus entityStatus);
}
