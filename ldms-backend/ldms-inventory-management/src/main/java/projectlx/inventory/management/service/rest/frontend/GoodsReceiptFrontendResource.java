package projectlx.inventory.management.service.rest.frontend;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import projectlx.inventory.management.service.processor.api.GoodsReceiptServiceProcessor;
import projectlx.inventory.management.utils.requests.ReceiveGoodsRequest;
import projectlx.inventory.management.utils.responses.PurchaseOrderResponse;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;
import java.util.Locale;

@CrossOrigin
@RestController
@RequestMapping("/ldms-inventory-management/v1/frontend/purchase-orders")
@Tag(name = "Goods Receipt Frontend Resource", description = "Operations related to receiving goods against purchase orders (frontend)")
@RequiredArgsConstructor
public class GoodsReceiptFrontendResource {

    private final GoodsReceiptServiceProcessor goodsReceiptServiceProcessor;
    private static final Logger logger = LoggerFactory.getLogger(GoodsReceiptFrontendResource.class);

    @Auditable(action = "RECEIVE_GOODS")
    @PostMapping("/{purchaseOrderId}/receive")
    @Operation(summary = "Receive goods for a purchase order")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Goods received successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "404", description = "Purchase order not found")
    })
    public PurchaseOrderResponse receive(@PathVariable("purchaseOrderId") final Long purchaseOrderId,
                                                         @Valid @RequestBody final ReceiveGoodsRequest request,
                                                         @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                                         @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
                                                         final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        logger.info("Incoming FE request to receive goods for PO {} by {}", purchaseOrderId, username);
        request.setPurchaseOrderId(purchaseOrderId);
        return goodsReceiptServiceProcessor.receiveGoods(request, locale, username);
    }
}
