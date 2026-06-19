package projectlx.inventory.management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import projectlx.inventory.management.model.InventoryIntegrationCredential;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.List;
import java.util.Optional;

public interface InventoryIntegrationCredentialRepository
        extends JpaRepository<InventoryIntegrationCredential, Long>,
                JpaSpecificationExecutor<InventoryIntegrationCredential> {

    Optional<InventoryIntegrationCredential> findByApiKeyAndEntityStatusNot(
            String apiKey, EntityStatus entityStatus);

    Optional<InventoryIntegrationCredential> findByIdAndEntityStatusNot(Long id, EntityStatus entityStatus);

    List<InventoryIntegrationCredential> findByOrganizationIdAndEntityStatusNot(
            Long organizationId, EntityStatus entityStatus);

    boolean existsByApiKey(String apiKey);
}
