package projectlx.inventory.management.business.logic.support;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.inventory.management.business.logic.api.InventoryItemService;
import projectlx.inventory.management.model.InventoryItem;
import projectlx.inventory.management.model.Product;
import projectlx.inventory.management.model.ReferenceDocumentType;
import projectlx.inventory.management.model.WarehouseLocation;
import projectlx.inventory.management.repository.InventoryItemRepository;
import projectlx.inventory.management.utils.requests.CreateOrUpdateStockRequest;
import projectlx.inventory.management.utils.responses.InventoryItemResponse;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class StockTransferSupport {

    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryItemService inventoryItemService;

    public void transferStock(Product product,
                              WarehouseLocation fromWarehouse,
                              WarehouseLocation toWarehouse,
                              BigDecimal quantity,
                              InventoryItem sourceTemplate,
                              Long userId,
                              Long referenceDocumentId,
                              ReferenceDocumentType referenceDocumentType,
                              BigDecimal unitCost,
                              String reason,
                              Locale locale,
                              String username) {
        Optional<InventoryItem> sourceItemOpt = inventoryItemRepository
                .findByProductIdAndWarehouseLocationIdAndEntityStatusNot(
                        product.getId(), fromWarehouse.getId(), EntityStatus.DELETED);
        if (sourceItemOpt.isEmpty()) {
            throw new IllegalArgumentException("Source inventory not found for transfer.");
        }

        InventoryItemResponse stockOutResponse = inventoryItemService.createStockOut(
                sourceItemOpt.get().getId(),
                quantity,
                reason + " (out from " + fromWarehouse.getName() + ")",
                userId,
                referenceDocumentId,
                referenceDocumentType,
                locale,
                username);
        if (!stockOutResponse.isSuccess()) {
            throw new IllegalStateException(stockOutResponse.getMessage());
        }

        ensureInventoryItem(product, toWarehouse, sourceTemplate, userId);

        CreateOrUpdateStockRequest stockIn = new CreateOrUpdateStockRequest();
        stockIn.setProductId(product.getId());
        stockIn.setWarehouseLocationId(toWarehouse.getId());
        stockIn.setQuantityReceived(quantity);
        stockIn.setReferenceDocumentId(referenceDocumentId);
        stockIn.setReferenceDocumentType(referenceDocumentType);
        stockIn.setUpdatedByUserId(userId);
        stockIn.setUnitCost(unitCost);
        stockIn.setReason(reason + " (in to " + toWarehouse.getName() + ")");
        inventoryItemService.createOrUpdateStock(stockIn, locale, username);
    }

    private void ensureInventoryItem(Product product, WarehouseLocation warehouse, InventoryItem template, Long userId) {
        Optional<InventoryItem> existing = inventoryItemRepository
                .findByProductIdAndWarehouseLocationIdAndEntityStatusNot(
                        product.getId(), warehouse.getId(), EntityStatus.DELETED);
        if (existing.isPresent()) {
            return;
        }
        InventoryItem item = new InventoryItem();
        item.setProduct(product);
        item.setWarehouseLocation(warehouse);
        item.setSupplierId(template.getSupplierId());
        item.setBatchLot(template.getBatchLot());
        item.setSerialNumber(template.getSerialNumber());
        item.setExpiresAt(template.getExpiresAt());
        item.setMinStockLevel(template.getMinStockLevel());
        item.setReorderQuantity(template.getReorderQuantity());
        item.setLastPurchaseCost(template.getLastPurchaseCost());
        item.setCreatedByUserId(userId);
        item.setQuantity(BigDecimal.ZERO);
        item.setCurrentStock(BigDecimal.ZERO);
        item.setReservedQuantity(BigDecimal.ZERO);
        item.setTotalCost(BigDecimal.ZERO);
        item.setAverageCost(template.getAverageCost());
        item.setUnitCost(template.getUnitCost());
        item.setEntityStatus(EntityStatus.ACTIVE);
        inventoryItemRepository.save(item);
    }
}
