package projectlx.inventory.management.business.auditable.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import projectlx.inventory.management.business.auditable.api.SalesOrderLineServiceAuditable;
import projectlx.inventory.management.model.SalesOrderLine;
import projectlx.inventory.management.repository.SalesOrderLineRepository;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class SalesOrderLineServiceAuditableImpl implements SalesOrderLineServiceAuditable {

    private final SalesOrderLineRepository salesOrderLineRepository;

    @Override
    public SalesOrderLine create(SalesOrderLine salesOrderLine, Locale locale, String username) {
        return salesOrderLineRepository.save(salesOrderLine);
    }

    @Override
    public SalesOrderLine update(SalesOrderLine salesOrderLine, Locale locale, String username) {
        return salesOrderLineRepository.save(salesOrderLine);
    }

    @Override
    public SalesOrderLine delete(SalesOrderLine salesOrderLine, Locale locale) {
        return salesOrderLineRepository.save(salesOrderLine);
    }
}
