package projectlx.inventory.management.business.logic.api;

import com.lowagie.text.DocumentException;
import projectlx.inventory.management.model.InventoryItem;
import projectlx.inventory.management.model.ReferenceDocumentType;
import projectlx.inventory.management.model.WarehouseLocation;
import projectlx.inventory.management.utils.dtos.InventoryItemDto;
import projectlx.inventory.management.utils.dtos.ImportSummary;
import projectlx.inventory.management.utils.requests.CreateInitialStockRequest;
import projectlx.inventory.management.utils.requests.InventoryItemMultipleFiltersRequest;
import projectlx.inventory.management.utils.requests.CreateInventoryItemRequest;
import projectlx.inventory.management.utils.requests.EditInventoryItemRequest;
import projectlx.inventory.management.utils.requests.CreateOrUpdateStockRequest;
import projectlx.inventory.management.utils.responses.InventoryItemResponse;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public interface InventoryItemService {
    InventoryItemResponse create(CreateInventoryItemRequest request, Locale locale, String username);
    Optional<WarehouseLocation> findWarehouseLocationBySupplierId(Long supplierId, Long warehouseLocationId);
    InventoryItemResponse findById(Long id, Locale locale, String username);
    InventoryItemResponse findAllAsList(Locale locale, String username);
    InventoryItemResponse update(EditInventoryItemRequest request, String username, Locale locale);
    InventoryItemResponse delete(Long id, Locale locale, String username);
    InventoryItemResponse findByMultipleFilters(InventoryItemMultipleFiltersRequest request, String username, Locale locale);
    void createOrUpdateStock(CreateOrUpdateStockRequest request, Locale locale, String username);
    InventoryItemResponse createStockOut(Long inventoryItemId, BigDecimal quantity, String reason, Long userId,
                                         Long referenceDocumentId, ReferenceDocumentType referenceDocumentType, Locale locale,
                                         String username);
    InventoryItemResponse recordPurchaseReturn(Long inventoryItemId, BigDecimal quantityReturned, String reason,
                                               Long userId, Long referenceDocumentId, ReferenceDocumentType referenceDocumentType,
                                               BigDecimal unitCost, Locale locale, String username);
    Optional<InventoryItem> findInventoryItemByProductIdAndWarehouseId(Long productId, Long warehouseId);
    byte[] exportToCsv(List<InventoryItemDto> items);
    byte[] exportToExcel(List<InventoryItemDto> items) throws IOException;
    byte[] exportToPdf(List<InventoryItemDto> items) throws DocumentException;
    ImportSummary importInventoryItemFromCsv(InputStream csvInputStream) throws IOException;
    InventoryItemResponse createInitialStock(CreateInitialStockRequest request, Locale locale, String username);
    InventoryItemResponse createInitialStockBulk(List<CreateInitialStockRequest> requests,
                                                 Locale locale, String username);
}
