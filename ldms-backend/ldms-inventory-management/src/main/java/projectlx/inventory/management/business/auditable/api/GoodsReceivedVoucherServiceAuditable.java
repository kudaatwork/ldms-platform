package projectlx.inventory.management.business.auditable.api;

import projectlx.inventory.management.model.GoodsReceivedVoucher;

import java.util.Locale;

public interface GoodsReceivedVoucherServiceAuditable {
    GoodsReceivedVoucher create(GoodsReceivedVoucher goodsReceivedVoucher, Locale locale, String username);
    GoodsReceivedVoucher update(GoodsReceivedVoucher goodsReceivedVoucher, Locale locale, String username);
    GoodsReceivedVoucher delete(GoodsReceivedVoucher goodsReceivedVoucher, Locale locale);
}