package projectlx.inventory.management.business.auditable.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import projectlx.inventory.management.business.auditable.api.StockAdjustmentServiceAuditable;
import projectlx.inventory.management.model.StockAdjustment;
import projectlx.inventory.management.repository.StockAdjustmentRepository;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class StockAdjustmentServiceAuditableImpl implements StockAdjustmentServiceAuditable {

    private final StockAdjustmentRepository stockAdjustmentRepository;

    @Override
    public StockAdjustment create(StockAdjustment stockAdjustment, Locale locale, String username) {
        return stockAdjustmentRepository.save(stockAdjustment);
    }

    @Override
    public StockAdjustment update(StockAdjustment stockAdjustment, Locale locale, String username) {
        return stockAdjustmentRepository.save(stockAdjustment);
    }

    @Override
    public StockAdjustment delete(StockAdjustment stockAdjustment, Locale locale) {
        return stockAdjustmentRepository.save(stockAdjustment);
    }
}
