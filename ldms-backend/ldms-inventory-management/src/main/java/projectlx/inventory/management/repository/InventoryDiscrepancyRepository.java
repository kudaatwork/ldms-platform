package projectlx.inventory.management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import projectlx.inventory.management.model.InventoryDiscrepancy;

public interface InventoryDiscrepancyRepository extends JpaRepository<InventoryDiscrepancy, Long>, JpaSpecificationExecutor<InventoryDiscrepancy> {
}
