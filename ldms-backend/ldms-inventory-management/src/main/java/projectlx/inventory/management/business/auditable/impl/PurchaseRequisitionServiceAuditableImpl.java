package projectlx.inventory.management.business.auditable.impl;

import lombok.RequiredArgsConstructor;
import projectlx.inventory.management.business.auditable.api.PurchaseRequisitionServiceAuditable;
import projectlx.inventory.management.model.PurchaseRequisition;
import projectlx.inventory.management.model.PurchaseRequisitionAmendment;
import projectlx.inventory.management.repository.PurchaseRequisitionAmendmentRepository;
import projectlx.inventory.management.repository.PurchaseRequisitionRepository;

import java.util.Locale;

@RequiredArgsConstructor
public class PurchaseRequisitionServiceAuditableImpl implements PurchaseRequisitionServiceAuditable {

    private final PurchaseRequisitionRepository purchaseRequisitionRepository;
    private final PurchaseRequisitionAmendmentRepository purchaseRequisitionAmendmentRepository;

    @Override
    public PurchaseRequisition create(PurchaseRequisition purchaseRequisition, Locale locale, String username) {
        return purchaseRequisitionRepository.save(purchaseRequisition);
    }

    @Override
    public PurchaseRequisition update(PurchaseRequisition purchaseRequisition, Locale locale, String username) {
        return purchaseRequisitionRepository.save(purchaseRequisition);
    }

    @Override
    public PurchaseRequisition delete(PurchaseRequisition purchaseRequisition, Locale locale) {
        return purchaseRequisitionRepository.save(purchaseRequisition);
    }

    @Override
    public PurchaseRequisitionAmendment createAmendment(PurchaseRequisitionAmendment amendment, Locale locale, String username) {
        return purchaseRequisitionAmendmentRepository.save(amendment);
    }
}
