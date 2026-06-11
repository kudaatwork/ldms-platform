package projectlx.inventory.management.business.auditable.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import projectlx.inventory.management.business.auditable.api.InventoryItemServiceAuditable;
import projectlx.inventory.management.model.InventoryItem;
import projectlx.inventory.management.repository.InventoryItemRepository;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class InventoryItemServiceAuditableImpl implements InventoryItemServiceAuditable {

    private final InventoryItemRepository inventoryItemRepository;

    @Override
    public InventoryItem create(InventoryItem inventoryItem, Locale locale, String username) {
        return inventoryItemRepository.save(inventoryItem);
    }

    @Override
    public InventoryItem update(InventoryItem inventoryItem, Locale locale, String username) {
        return inventoryItemRepository.save(inventoryItem);
    }

    @Override
    public InventoryItem delete(InventoryItem inventoryItem, Locale locale) {
        return inventoryItemRepository.save(inventoryItem);
    }
}
