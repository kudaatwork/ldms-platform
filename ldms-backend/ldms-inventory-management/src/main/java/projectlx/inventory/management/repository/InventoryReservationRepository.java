package projectlx.inventory.management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import projectlx.inventory.management.model.InventoryReservation;
import projectlx.inventory.management.model.ReservationStatus;

import java.util.List;
import java.util.Optional;

public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, Long>, JpaSpecificationExecutor<InventoryReservation> {

    List<InventoryReservation> findBySalesOrderId(Long salesOrderId);

    List<InventoryReservation> findBySalesOrderIdAndStatus(Long salesOrderId, ReservationStatus status);

    List<InventoryReservation> findByStatus(ReservationStatus status);

    List<InventoryReservation> findBySalesOrderIdAndSalesOrderLineIdOrderByCreatedAtAsc(Long salesOrderId, Long salesOrderLineId);

    List<InventoryReservation> findBySalesOrderIdAndSalesOrderLineIdAndWarehouseLocation_IdOrderByCreatedAtAsc(Long salesOrderId, Long salesOrderLineId, Long warehouseLocationId);

    List<InventoryReservation> findBySalesOrderIdAndSalesOrderLineIdAndWarehouseLocation_IdAndStatusOrderByCreatedAtAsc(Long salesOrderId, Long salesOrderLineId, Long warehouseLocationId, ReservationStatus status);

    Optional<InventoryReservation> findBySalesOrderLineIdAndProduct_IdAndWarehouseLocation_Id(Long salesOrderLineId, Long productId, Long warehouseLocationId);
}
