package projectlx.inventory.management.business.auditable.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import projectlx.inventory.management.business.auditable.api.PurchaseReturnServiceAuditable;
import projectlx.inventory.management.model.PurchaseReturn;
import projectlx.inventory.management.repository.PurchaseReturnRepository;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PurchaseReturnServiceAuditableImpl implements PurchaseReturnServiceAuditable {

    private final PurchaseReturnRepository purchaseReturnRepository;

    @Override
    public PurchaseReturn create(PurchaseReturn purchaseReturn, Locale locale, String username) {
        return purchaseReturnRepository.save(purchaseReturn);
    }

    @Override
    public PurchaseReturn update(PurchaseReturn purchaseReturn, Locale locale, String username) {
        return purchaseReturnRepository.save(purchaseReturn);
    }

    @Override
    public PurchaseReturn delete(PurchaseReturn purchaseReturn, Locale locale) {
        return purchaseReturnRepository.save(purchaseReturn);
    }
}