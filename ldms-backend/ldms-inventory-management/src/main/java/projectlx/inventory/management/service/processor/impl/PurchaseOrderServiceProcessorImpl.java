package projectlx.inventory.management.service.processor.impl;

import com.lowagie.text.DocumentException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import projectlx.inventory.management.business.logic.api.PurchaseOrderService;
import projectlx.inventory.management.service.processor.api.PurchaseOrderServiceProcessor;
import projectlx.inventory.management.utils.dtos.PurchaseOrderDto;
import projectlx.inventory.management.utils.dtos.ImportSummary;
import projectlx.inventory.management.utils.requests.CreatePurchaseOrderRequest;
import projectlx.inventory.management.utils.requests.EditPurchaseOrderRequest;
import projectlx.inventory.management.utils.requests.PurchaseOrderMultipleFiltersRequest;
import projectlx.inventory.management.utils.requests.ReceiveGoodsRequest;
import projectlx.inventory.management.utils.responses.PurchaseOrderResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PurchaseOrderServiceProcessorImpl implements PurchaseOrderServiceProcessor {

    private final PurchaseOrderService purchaseOrderService;
    private static final Logger logger = LoggerFactory.getLogger(PurchaseOrderServiceProcessorImpl.class);

    @Override
    public PurchaseOrderResponse create(CreatePurchaseOrderRequest request, Locale locale, String username) {
        logger.info("Incoming request to create purchase order for user: {}", username);
        PurchaseOrderResponse response = purchaseOrderService.create(request, locale, username);
        logger.info("Outgoing response after creating purchase order: Success: {}", response != null &&
                response.isSuccess());
        return response;
    }

    @Override
    public PurchaseOrderResponse findById(Long id, Locale locale, String username) {
        logger.info("Incoming request to find purchase order by ID: {} for user: {}", id, username);
        PurchaseOrderResponse response = purchaseOrderService.findById(id, locale, username);
        logger.info("Outgoing response after finding purchase order by ID: Success: {}", response != null &&
                response.isSuccess());
        return response;
    }

    @Override
    public PurchaseOrderResponse findAllAsList(Locale locale, String username) {
        logger.info("Incoming request to find all purchase orders as list for user: {}", username);
        PurchaseOrderResponse response = purchaseOrderService.findAllAsList(locale, username);
        logger.info("Outgoing response after finding all purchase orders: Success: {}", response != null &&
                response.isSuccess());
        return response;
    }

    @Override
    public PurchaseOrderResponse update(EditPurchaseOrderRequest request, String username, Locale locale) {
        logger.info("Incoming request to update purchase order for user: {}", username);
        PurchaseOrderResponse response = purchaseOrderService.update(request, username, locale);
        logger.info("Outgoing response after updating purchase order: Success: {}", response != null &&
                response.isSuccess());
        return response;
    }

    @Override
    public PurchaseOrderResponse delete(Long id, Locale locale, String username) {
        logger.info("Incoming request to delete purchase order by ID: {} for user: {}", id, username);
        PurchaseOrderResponse response = purchaseOrderService.delete(id, locale, username);
        logger.info("Outgoing response after deleting purchase order: Success: {}", response != null &&
                response.isSuccess());
        return response;
    }

    @Override
    public PurchaseOrderResponse findByMultipleFilters(PurchaseOrderMultipleFiltersRequest request,
                                                       String username, Locale locale) {
        logger.info("Incoming request to find purchase orders by multiple filters for user: {}", username);
        PurchaseOrderResponse response = purchaseOrderService.findByMultipleFilters(request, username, locale);
        logger.info("Outgoing response after finding purchase orders by filters: Success: {}", response != null &&
                response.isSuccess());
        return response;
    }

    @Override
    public PurchaseOrderResponse receiveGoods(ReceiveGoodsRequest request, String username, Locale locale) {
        logger.info("Incoming request to receive goods for user: {}", username);
        PurchaseOrderResponse response = purchaseOrderService.receiveGoods(request, username, locale);
        logger.info("Outgoing response after receiving goods: Success: {}", response != null && response.isSuccess());
        return response;
    }

    @Override
    public byte[] exportToCsv(List<PurchaseOrderDto> items) {
        logger.info("Incoming request to export {} purchase orders to CSV", items != null ? items.size() : 0);
        byte[] result = purchaseOrderService.exportToCsv(items);
        logger.info("Outgoing response after exporting to CSV: Size: {} bytes", result != null ? result.length : 0);
        return result;
    }

    @Override
    public byte[] exportToExcel(List<PurchaseOrderDto> items) throws IOException {
        logger.info("Incoming request to export {} purchase orders to Excel", items != null ? items.size() : 0);
        byte[] result = purchaseOrderService.exportToExcel(items);
        logger.info("Outgoing response after exporting to Excel: Size: {} bytes", result != null ? result.length : 0);
        return result;
    }

    @Override
    public byte[] exportToPdf(List<PurchaseOrderDto> items) throws DocumentException {
        logger.info("Incoming request to export {} purchase orders to PDF", items != null ? items.size() : 0);
        byte[] result = purchaseOrderService.exportToPdf(items);
        logger.info("Outgoing response after exporting to PDF: Size: {} bytes", result != null ? result.length : 0);
        return result;
    }

    @Override
    public ImportSummary importPurchaseOrderFromCsv(InputStream csvInputStream) throws IOException {
        logger.info("Incoming request to import purchase orders from CSV");
        ImportSummary result = purchaseOrderService.importPurchaseOrderFromCsv(csvInputStream);
        logger.info("Outgoing response after importing from CSV: Success: {}", result != null);
        return result;
    }
}
