package projectlx.inventory.management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import projectlx.inventory.management.model.GoodsReceivedVoucher;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.List;
import java.util.Optional;

public interface GoodsReceivedVoucherRepository extends JpaRepository<GoodsReceivedVoucher, Long>,
        JpaSpecificationExecutor<GoodsReceivedVoucher> {
    Optional<GoodsReceivedVoucher> findByIdAndEntityStatusNot(Long grvId, EntityStatus entityStatus);

    // NEW: Find GRV by number
    Optional<GoodsReceivedVoucher> findByGrvNumberAndEntityStatusNot(String grvNumber, EntityStatus entityStatus);

    // NEW: Find all GRVs for a specific Purchase Order
    List<GoodsReceivedVoucher> findByPurchaseOrderIdAndEntityStatusNot(Long purchaseOrderId, EntityStatus entityStatus);
}
