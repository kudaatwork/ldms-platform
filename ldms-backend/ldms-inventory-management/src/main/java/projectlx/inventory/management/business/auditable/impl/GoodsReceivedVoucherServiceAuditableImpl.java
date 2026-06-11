package projectlx.inventory.management.business.auditable.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import projectlx.inventory.management.business.auditable.api.GoodsReceivedVoucherServiceAuditable;
import projectlx.inventory.management.model.GoodsReceivedVoucher;
import projectlx.inventory.management.repository.GoodsReceivedVoucherRepository;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class GoodsReceivedVoucherServiceAuditableImpl implements GoodsReceivedVoucherServiceAuditable {

    private final GoodsReceivedVoucherRepository goodsReceivedVoucherRepository;

    @Override
    public GoodsReceivedVoucher create(GoodsReceivedVoucher goodsReceivedVoucher, Locale locale, String username) {
        return goodsReceivedVoucherRepository.save(goodsReceivedVoucher);
    }

    @Override
    public GoodsReceivedVoucher update(GoodsReceivedVoucher goodsReceivedVoucher, Locale locale, String username) {
        return goodsReceivedVoucherRepository.save(goodsReceivedVoucher);
    }

    @Override
    public GoodsReceivedVoucher delete(GoodsReceivedVoucher goodsReceivedVoucher, Locale locale) {
        return goodsReceivedVoucherRepository.save(goodsReceivedVoucher);
    }
}