package projectlx.inventory.management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import projectlx.inventory.management.model.CrossDockDispatch;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.List;
import java.util.Optional;

public interface CrossDockDispatchRepository
        extends JpaRepository<CrossDockDispatch, Long>, JpaSpecificationExecutor<CrossDockDispatch> {

    Optional<CrossDockDispatch> findByIdAndEntityStatusNot(Long id, EntityStatus entityStatus);

    Optional<CrossDockDispatch> findByExternalDispatchIdAndOrganizationIdAndEntityStatusNot(
            String externalDispatchId, Long organizationId, EntityStatus entityStatus);

    List<CrossDockDispatch> findByOrganizationIdAndEntityStatusNot(
            Long organizationId, EntityStatus entityStatus);
}
