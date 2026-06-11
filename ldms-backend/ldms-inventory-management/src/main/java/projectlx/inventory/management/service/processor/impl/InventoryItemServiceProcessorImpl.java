package projectlx.inventory.management.service.processor.impl;

import com.lowagie.text.DocumentException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import projectlx.inventory.management.business.logic.api.InventoryItemService;
import projectlx.inventory.management.service.processor.api.InventoryItemServiceProcessor;
import projectlx.inventory.management.utils.dtos.InventoryItemDto;
import projectlx.inventory.management.utils.dtos.ImportSummary;
import projectlx.inventory.management.utils.requests.CreateInitialStockRequest;
import projectlx.inventory.management.utils.requests.CreateInventoryItemRequest;
import projectlx.inventory.management.utils.requests.EditInventoryItemRequest;
import projectlx.inventory.management.utils.requests.InventoryItemMultipleFiltersRequest;
import projectlx.inventory.management.utils.responses.InventoryItemResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class InventoryItemServiceProcessorImpl implements InventoryItemServiceProcessor {

    private final InventoryItemService inventoryItemService;
    private static final Logger logger = LoggerFactory.getLogger(InventoryItemServiceProcessorImpl.class);

    @Override
    public InventoryItemResponse create(CreateInventoryItemRequest request, Locale locale, String username) {
        logger.info("Incoming request to create inventory item for user: {}", username);

        InventoryItemResponse response = inventoryItemService.create(request, locale, username);

        logger.info("Outgoing response after creating inventory item: Success: {}", 
                response != null ? response.isSuccess() : false);

        return response;
    }

    @Override
    public InventoryItemResponse findById(Long id, Locale locale, String username) {
        logger.info("Incoming request to find inventory item by ID: {} for user: {}", id, username);

        InventoryItemResponse response = inventoryItemService.findById(id, locale, username);

        logger.info("Outgoing response after finding inventory item by ID: Success: {}", 
                response != null ? response.isSuccess() : false);

        return response;
    }

    @Override
    public InventoryItemResponse findAllAsList(Locale locale, String username) {
        logger.info("Incoming request to find all inventory items as list for user: {}", username);

        InventoryItemResponse response = inventoryItemService.findAllAsList(locale, username);

        logger.info("Outgoing response after finding all inventory items: Success: {}", 
                response != null ? response.isSuccess() : false);

        return response;
    }

    @Override
    public InventoryItemResponse update(EditInventoryItemRequest request, String username, Locale locale) {
        logger.info("Incoming request to update inventory item for user: {}", username);

        InventoryItemResponse response = inventoryItemService.update(request, username, locale);

        logger.info("Outgoing response after updating inventory item: Success: {}", 
                response != null ? response.isSuccess() : false);

        return response;
    }

    @Override
    public InventoryItemResponse delete(Long id, Locale locale, String username) {
        logger.info("Incoming request to delete inventory item by ID: {} for user: {}", id, username);

        InventoryItemResponse response = inventoryItemService.delete(id, locale, username);

        logger.info("Outgoing response after deleting inventory item: Success: {}", 
                response != null ? response.isSuccess() : false);

        return response;
    }

    @Override
    public InventoryItemResponse findByMultipleFilters(InventoryItemMultipleFiltersRequest request, String username, Locale locale) {
        logger.info("Incoming request to find inventory items by multiple filters for user: {}", username);

        InventoryItemResponse response = inventoryItemService.findByMultipleFilters(request, username, locale);

        logger.info("Outgoing response after finding inventory items by filters: Success: {}", 
                response != null ? response.isSuccess() : false);

        return response;
    }

    @Override
    public byte[] exportToCsv(List<InventoryItemDto> items) {
        logger.info("Incoming request to export {} inventory items to CSV", 
                items != null ? items.size() : 0);

        byte[] result = inventoryItemService.exportToCsv(items);

        logger.info("Outgoing response after exporting to CSV: Size: {} bytes", 
                result != null ? result.length : 0);

        return result;
    }

    @Override
    public byte[] exportToExcel(List<InventoryItemDto> items) throws IOException {
        logger.info("Incoming request to export {} inventory items to Excel", 
                items != null ? items.size() : 0);

        byte[] result = inventoryItemService.exportToExcel(items);

        logger.info("Outgoing response after exporting to Excel: Size: {} bytes", 
                result != null ? result.length : 0);

        return result;
    }

    @Override
    public byte[] exportToPdf(List<InventoryItemDto> items) throws DocumentException {
        logger.info("Incoming request to export {} inventory items to PDF", 
                items != null ? items.size() : 0);

        byte[] result = inventoryItemService.exportToPdf(items);

        logger.info("Outgoing response after exporting to PDF: Size: {} bytes", 
                result != null ? result.length : 0);

        return result;
    }

    @Override
    public ImportSummary importInventoryItemFromCsv(InputStream csvInputStream) throws IOException {
        logger.info("Incoming request to import inventory items from CSV");

        ImportSummary result = inventoryItemService.importInventoryItemFromCsv(csvInputStream);

        logger.info("Outgoing response after importing from CSV: imported={}, failed={}, total={}",
                result != null ? result.success : 0,
                result != null ? result.failed : 0,
                result != null ? result.total : 0);

        return result;
    }

    @Override
    public InventoryItemResponse createInitialStock(CreateInitialStockRequest request, Locale locale, String username) {
        logger.info("Incoming request to create initial stock for user: {}", username);

        InventoryItemResponse response = inventoryItemService.createInitialStock(request, locale, username);

        logger.info("Outgoing response after creating initial stock: Success: {}",
                response != null ? response.isSuccess() : false);

        return response;
    }

    @Override
    public InventoryItemResponse createInitialStockBulk(List<CreateInitialStockRequest> requests, Locale locale, String username) {
        logger.info("Incoming request to create initial stock in bulk. Requests count: {} for user: {}",
                requests != null ? requests.size() : 0, username);

        InventoryItemResponse response = inventoryItemService.createInitialStockBulk(requests, locale, username);

        logger.info("Outgoing response after creating initial stock bulk: Success: {}",
                response != null ? response.isSuccess() : false);

        return response;
    }
}
