package projectlx.inventory.management.batch.writers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import projectlx.inventory.management.model.SalesReservation;
import projectlx.inventory.management.repository.SalesReservationRepository;
import projectlx.inventory.management.repository.InventoryItemRepository;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

@RequiredArgsConstructor
@Slf4j
public class ExpiredReservationWriter implements ItemWriter<SalesReservation> {

    private final SalesReservationRepository salesReservationRepository;
    private final InventoryItemRepository inventoryItemRepository;

    @Override
    public void write(Chunk<? extends SalesReservation> chunk) throws Exception {
        for (SalesReservation reservation : chunk) {
            salesReservationRepository.save(reservation);
            releaseInventoryReservation(reservation);
            log.info("Expired reservation {} processed and inventory released",
                    reservation.getReservationNumber());
        }
    }

    private void releaseInventoryReservation(SalesReservation reservation) {
        inventoryItemRepository.findByProductIdAndWarehouseLocationIdAndEntityStatusNot(
                reservation.getProduct().getId(),
                reservation.getWarehouseLocation().getId(),
                EntityStatus.DELETED
        ).ifPresent(inventoryItem -> {
            // Use the built-in helper method to safely release a reserved quantity
            inventoryItem.releaseReservedQuantity(reservation.getQuantityReserved());
            inventoryItemRepository.save(inventoryItem);
        });
    }
}