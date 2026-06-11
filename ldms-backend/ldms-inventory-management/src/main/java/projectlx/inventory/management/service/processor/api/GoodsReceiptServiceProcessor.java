package projectlx.inventory.management.service.processor.api;

import projectlx.inventory.management.utils.requests.ReceiveGoodsRequest;
import projectlx.inventory.management.utils.responses.PurchaseOrderResponse;

import java.util.Locale;

public interface GoodsReceiptServiceProcessor {
    PurchaseOrderResponse receiveGoods(ReceiveGoodsRequest request, Locale locale, String username);
}
