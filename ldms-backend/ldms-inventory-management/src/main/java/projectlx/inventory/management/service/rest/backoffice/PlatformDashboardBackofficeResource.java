package projectlx.inventory.management.service.rest.backoffice;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;
import projectlx.inventory.management.service.processor.api.PurchaseOrderServiceProcessor;
import projectlx.inventory.management.utils.responses.PurchaseOrderResponse;

import java.util.Locale;

@CrossOrigin
@RestController
@RequestMapping("/ldms-inventory-management/v1/backoffice/platform-dashboard")
@Tag(name = "Platform dashboard (backoffice)", description = "Cross-tenant inventory metrics for LX administrators")
@RequiredArgsConstructor
public class PlatformDashboardBackofficeResource {

    private final PurchaseOrderServiceProcessor purchaseOrderServiceProcessor;

    @Auditable(action = "BACKOFFICE_PLATFORM_PURCHASE_ORDER_SEARCH")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/purchase-orders/search")
    @Operation(summary = "Cross-tenant purchase order search for LX Admin")
    public ResponseEntity<PurchaseOrderResponse> searchPurchaseOrders(
            @RequestParam String term,
            @RequestParam(defaultValue = "25") int limit,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        PurchaseOrderResponse response = purchaseOrderServiceProcessor.searchForPlatformDashboard(term, limit, locale);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }
}
