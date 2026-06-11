package projectlx.inventory.management.business.auditable.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import projectlx.inventory.management.business.auditable.api.PurchaseOrderLineServiceAuditable;
import projectlx.inventory.management.model.PurchaseOrderLine;
import projectlx.inventory.management.repository.PurchaseOrderLineRepository;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PurchaseOrderLineServiceAuditableImpl implements PurchaseOrderLineServiceAuditable {

    private final PurchaseOrderLineRepository purchaseOrderLineRepository;

    @Override
    public PurchaseOrderLine create(PurchaseOrderLine purchaseOrderLine, Locale locale, String username) {
        return purchaseOrderLineRepository.save(purchaseOrderLine);
    }

    @Override
    public PurchaseOrderLine update(PurchaseOrderLine purchaseOrderLine, Locale locale, String username) {
        return purchaseOrderLineRepository.save(purchaseOrderLine);
    }

    @Override
    public PurchaseOrderLine delete(PurchaseOrderLine purchaseOrderLine, Locale locale) {
        return purchaseOrderLineRepository.save(purchaseOrderLine);
    }
}
