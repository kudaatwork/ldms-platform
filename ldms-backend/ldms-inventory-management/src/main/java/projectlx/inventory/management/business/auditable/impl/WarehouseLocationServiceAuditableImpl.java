package projectlx.inventory.management.business.auditable.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import projectlx.inventory.management.business.auditable.api.WarehouseLocationServiceAuditable;
import projectlx.inventory.management.model.WarehouseLocation;
import projectlx.inventory.management.repository.WarehouseLocationRepository;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class WarehouseLocationServiceAuditableImpl implements WarehouseLocationServiceAuditable {

    private final WarehouseLocationRepository warehouseLocationRepository;

    @Override
    public WarehouseLocation create(WarehouseLocation warehouseLocation, Locale locale, String username) {
        return warehouseLocationRepository.save(warehouseLocation);
    }

    @Override
    public WarehouseLocation update(WarehouseLocation warehouseLocation, Locale locale, String username) {
        return warehouseLocationRepository.save(warehouseLocation);
    }

    @Override
    public WarehouseLocation delete(WarehouseLocation warehouseLocation, Locale locale) {
        return warehouseLocationRepository.save(warehouseLocation);
    }
}
