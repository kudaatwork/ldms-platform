package projectlx.inventory.management.business.auditable.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import projectlx.inventory.management.business.auditable.api.SalesOrderServiceAuditable;
import projectlx.inventory.management.model.SalesOrder;
import projectlx.inventory.management.repository.SalesOrderRepository;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class SalesOrderServiceAuditableImpl implements SalesOrderServiceAuditable {

    private final SalesOrderRepository salesOrderRepository;

    @Override
    public SalesOrder create(SalesOrder salesOrder, Locale locale, String username) {
        return salesOrderRepository.save(salesOrder);
    }

    @Override
    public SalesOrder update(SalesOrder salesOrder, Locale locale, String username) {
        return salesOrderRepository.save(salesOrder);
    }

    @Override
    public SalesOrder delete(SalesOrder salesOrder, Locale locale) {
        return salesOrderRepository.save(salesOrder);
    }
}
