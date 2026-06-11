package projectlx.inventory.management.business.auditable.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import projectlx.inventory.management.business.auditable.api.PurchaseOrderServiceAuditable;
import projectlx.inventory.management.model.PurchaseOrder;
import projectlx.inventory.management.repository.PurchaseOrderRepository;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PurchaseOrderServiceAuditableImpl implements PurchaseOrderServiceAuditable {

    private final PurchaseOrderRepository purchaseOrderRepository;

    @Override
    public PurchaseOrder create(PurchaseOrder purchaseOrder, Locale locale, String username) {
        return purchaseOrderRepository.save(purchaseOrder);
    }

    @Override
    public PurchaseOrder update(PurchaseOrder purchaseOrder, Locale locale, String username) {
        return purchaseOrderRepository.save(purchaseOrder);
    }

    @Override
    public PurchaseOrder delete(PurchaseOrder purchaseOrder, Locale locale) {
        return purchaseOrderRepository.save(purchaseOrder);
    }
}
