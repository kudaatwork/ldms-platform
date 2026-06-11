package projectlx.inventory.management.business.logic.api;

import projectlx.inventory.management.utils.dtos.GoodsReceiptResult;
import projectlx.inventory.management.utils.requests.ReceiveGoodsRequest;
import java.util.Locale;

public interface GoodsReceiptProcessor {
    GoodsReceiptResult processGoodsReceipt(ReceiveGoodsRequest request, 
                                         String username, Locale locale);
}