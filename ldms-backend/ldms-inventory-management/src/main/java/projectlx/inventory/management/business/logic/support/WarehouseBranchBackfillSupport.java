package projectlx.inventory.management.business.logic.support;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import projectlx.co.zw.shared_library.utils.dtos.BranchDto;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.inventory.management.business.auditable.api.WarehouseLocationServiceAuditable;
import projectlx.inventory.management.model.WarehouseLocation;
import projectlx.inventory.management.repository.WarehouseLocationRepository;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
@Slf4j
public class WarehouseBranchBackfillSupport {

    private final WarehouseLocationRepository warehouseLocationRepository;
    private final WarehouseLocationServiceAuditable warehouseLocationServiceAuditable;
    private final BranchAllocationSupport branchAllocationSupport;

    @Transactional
    public int backfillMissingBranchLinks(Locale locale, String username) {
        AtomicInteger updated = new AtomicInteger();
        for (WarehouseLocation warehouse : warehouseLocationRepository.findByEntityStatusNotOrderByIdAsc(EntityStatus.DELETED)) {
            if (warehouse.isVirtualWarehouse() || warehouse.getBranchId() != null) {
                continue;
            }
            if (warehouse.getSupplierId() == null || warehouse.getSupplierId() <= 0) {
                continue;
            }
            BranchDto headOffice = branchAllocationSupport.findHeadOfficeBranch(warehouse.getSupplierId(), locale)
                    .orElse(null);
            if (headOffice == null || headOffice.getId() == null) {
                log.warn("Skipping warehouse {} — no head-office branch for org {}", warehouse.getId(), warehouse.getSupplierId());
                continue;
            }
            warehouse.setBranchId(headOffice.getId());
            warehouseLocationServiceAuditable.update(warehouse, locale, username);
            updated.incrementAndGet();
        }
        log.info("Backfilled branch_id on {} warehouse(s)", updated.get());
        return updated.get();
    }
}
