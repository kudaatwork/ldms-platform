package projectlx.inventory.management.service.processor.api;

import com.lowagie.text.DocumentException;
import projectlx.inventory.management.utils.dtos.PurchaseOrderDto;
import projectlx.inventory.management.utils.dtos.ImportSummary;
import projectlx.inventory.management.utils.requests.PurchaseOrderMultipleFiltersRequest;
import projectlx.inventory.management.utils.requests.CreatePurchaseOrderRequest;
import projectlx.inventory.management.utils.requests.EditPurchaseOrderRequest;
import projectlx.inventory.management.utils.requests.ReceiveGoodsRequest;
import projectlx.inventory.management.utils.responses.PurchaseOrderResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

public interface PurchaseOrderServiceProcessor {
    PurchaseOrderResponse create(CreatePurchaseOrderRequest request, Locale locale, String username);
    PurchaseOrderResponse findById(Long id, Locale locale, String username);
    PurchaseOrderResponse findAllAsList(Locale locale, String username);
    PurchaseOrderResponse update(EditPurchaseOrderRequest request, String username, Locale locale);
    PurchaseOrderResponse delete(Long id, Locale locale, String username);
    PurchaseOrderResponse findByMultipleFilters(PurchaseOrderMultipleFiltersRequest request, String username, Locale locale);
    PurchaseOrderResponse receiveGoods(ReceiveGoodsRequest request, String username, Locale locale);
    byte[] exportToCsv(List<PurchaseOrderDto> items);
    byte[] exportToExcel(List<PurchaseOrderDto> items) throws IOException;
    byte[] exportToPdf(List<PurchaseOrderDto> items) throws DocumentException;
    ImportSummary importPurchaseOrderFromCsv(InputStream csvInputStream) throws IOException;

    PurchaseOrderResponse searchForPlatformDashboard(String term, int limit, Locale locale);
}
