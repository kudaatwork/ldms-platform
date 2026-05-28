package projectlx.co.zw.organizationmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import projectlx.co.zw.organizationmanagement.model.Industry;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.List;
import java.util.Optional;

public interface IndustryRepository extends JpaRepository<Industry, Long>, JpaSpecificationExecutor<Industry> {

    Optional<Industry> findByIdAndEntityStatusNot(Long id, EntityStatus deleted);

    List<Industry> findByEntityStatusNotOrderByNameAsc(EntityStatus deleted);

    Optional<Industry> findByNameIgnoreCaseAndEntityStatusNot(String name, EntityStatus deleted);

    boolean existsByNameIgnoreCaseAndIdNotAndEntityStatusNot(String name, Long id, EntityStatus deleted);
}
