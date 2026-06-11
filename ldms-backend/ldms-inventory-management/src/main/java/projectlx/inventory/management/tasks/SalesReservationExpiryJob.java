package projectlx.inventory.management.tasks;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import projectlx.inventory.management.model.InventoryItem;
import projectlx.inventory.management.model.ReservationStatus;
import projectlx.inventory.management.model.SalesReservation;
import projectlx.inventory.management.repository.InventoryItemRepository;
import projectlx.inventory.management.repository.SalesReservationRepository;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled job that expires active sales reservations whose reservedUntil has passed
 * and decrements the corresponding InventoryItem.reservedQuantity accordingly.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SalesReservationExpiryJob {

    private final SalesReservationRepository salesReservationRepository;
    private final InventoryItemRepository inventoryItemRepository;

    @Value("${jobs.salesReservationExpiry.enabled:true}")
    private boolean enabled;

    @Value("${jobs.salesReservationExpiry.fixedDelayMs:600000}") // default 10 minutes
    private long fixedDelayMs;

    @Value("${jobs.salesReservationExpiry.batchSize:200}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${jobs.salesReservationExpiry.fixedDelayMs:600000}")
    @Transactional
    public void run() {
        if (!enabled) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        int processed = 0;
        int expired = 0;
        int adjusted = 0;
        int missingInventory = 0;
        int errors = 0;

        try {
            Page<SalesReservation> page = salesReservationRepository
                    .findByReservationStatusAndReservedUntilBeforeAndEntityStatusNot(
                            ReservationStatus.ACTIVE,
                            now,
                            EntityStatus.DELETED,
                            PageRequest.of(0, Math.max(1, batchSize))
                    );

            List<SalesReservation> reservations = page.getContent();
            if (reservations.isEmpty()) {
                log.debug("SalesReservationExpiryJob: No expired active reservations found.");
                return;
            }

            for (SalesReservation reservation : reservations) {
                processed++;
                try {
                    // Lock and adjust inventory reserved quantity
                    Long productId = reservation.getProductId();
                    Long warehouseId = reservation.getWarehouseLocationId();
                    BigDecimal qty = reservation.getQuantityReserved();

                    if (productId != null && warehouseId != null && qty != null) {
                        var invOpt = inventoryItemRepository
                                .findByProductIdAndWarehouseLocationIdWithLock(productId, warehouseId, EntityStatus.DELETED);
                        if (invOpt.isPresent()) {
                            InventoryItem item = invOpt.get();
                            try {
                                item.releaseReservedQuantity(qty);
                                inventoryItemRepository.save(item);
                                adjusted++;
                            } catch (Exception e) {
                                throw new RuntimeException("Failed to adjust reserved quantity for InventoryItem id=" + item.getId(), e);
                            }
                        } else {
                            // Inventory item not found; track and continue
                            missingInventory++;
                            log.warn("SalesReservationExpiryJob: InventoryItem not found for productId={}, warehouseId={} when expiring reservation id={}",
                                    productId, warehouseId, reservation.getId());
                        }
                    } else {
                        log.warn("SalesReservationExpiryJob: Missing linkage (product/warehouse/qty) for reservation id={}", reservation.getId());
                    }

                    // Mark reservation as expired
                    reservation.setReservationStatus(ReservationStatus.EXPIRED);
                    reservation.setUpdatedAt(LocalDateTime.now());
                    salesReservationRepository.save(reservation);
                    expired++;
                } catch (Exception ex) {
                    errors++;
                    log.error("SalesReservationExpiryJob: Error processing reservation id={}: {}", reservation.getId(), ex.getMessage(), ex);
                }
            }
        } finally {
            log.info("SalesReservationExpiryJob Summary: processed={}, expired={}, adjusted={}, missingInventoryItems={}, errors={}",
                    processed, expired, adjusted, missingInventory, errors);
        }
    }
}
