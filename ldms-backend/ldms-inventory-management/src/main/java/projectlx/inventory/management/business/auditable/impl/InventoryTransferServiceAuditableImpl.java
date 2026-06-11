package projectlx.inventory.management.business.auditable.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import projectlx.inventory.management.business.auditable.api.InventoryTransferServiceAuditable;
import projectlx.inventory.management.model.InventoryTransfer;
import projectlx.inventory.management.repository.InventoryTransferRepository;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class InventoryTransferServiceAuditableImpl implements InventoryTransferServiceAuditable {

    private final InventoryTransferRepository inventoryTransferRepository;

    @Override
    public InventoryTransfer create(InventoryTransfer inventoryTransfer, Locale locale, String username) {
        return inventoryTransferRepository.save(inventoryTransfer);
    }

    @Override
    public InventoryTransfer update(InventoryTransfer inventoryTransfer, Locale locale, String username) {
        return inventoryTransferRepository.save(inventoryTransfer);
    }

    @Override
    public InventoryTransfer delete(InventoryTransfer inventoryTransfer, Locale locale) {
        return inventoryTransferRepository.save(inventoryTransfer);
    }
}
