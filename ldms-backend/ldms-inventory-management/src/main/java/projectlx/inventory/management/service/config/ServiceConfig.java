package projectlx.inventory.management.service.config;

import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.ObjectProvider;
import projectlx.inventory.management.business.logic.api.*;
import projectlx.inventory.management.repository.PurchaseOrderRepository;
import projectlx.inventory.management.service.processor.api.*;
import projectlx.inventory.management.service.processor.impl.*;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

@Configuration
public class ServiceConfig {

    @Bean
    public GoodsReceiptServiceProcessor goodsReceiptServiceProcessor(GoodsReceiptProcessor goodsReceiptProcessor,
                                                                     PurchaseOrderRepository purchaseOrderRepository,
                                                                     PurchaseOrderStatusManager purchaseOrderStatusManager,
                                                                     ModelMapper modelMapper,
                                                                     MessageService messageService) {
        return new GoodsReceiptServiceProcessorImpl(goodsReceiptProcessor, purchaseOrderRepository,
                purchaseOrderStatusManager, modelMapper, messageService);
    }

    @Bean
    public InventoryAllocationServiceProcessor inventoryAllocationServiceProcessor(InventoryAllocationService inventoryAllocationService) {
        return new InventoryAllocationServiceProcessorImpl(inventoryAllocationService);
    }

    @Bean
    public InventoryItemServiceProcessor inventoryItemServiceProcessor(InventoryItemService inventoryItemService) {
        return new InventoryItemServiceProcessorImpl(inventoryItemService);
    }

    @Bean
    public InventoryTransferServiceProcessor inventoryTransferServiceProcessor(InventoryTransferService inventoryTransferService) {
        return new InventoryTransferServiceProcessorImpl(inventoryTransferService);
    }

    @Bean
    public ProductCategoryServiceProcessor productCategoryServiceProcessor(ProductCategoryService productCategoryService) {
        return new ProductCategoryServiceProcessorImpl(productCategoryService);
    }

    @Bean
    public ProductDocumentServiceProcessor productDocumentServiceProcessor(ProductDocumentService productDocumentService) {
        return new ProductDocumentServiceProcessorImpl(productDocumentService);
    }

    @Bean
    public ProductServiceProcessor productServiceProcessor(ProductService productService) {
        return new ProductServiceProcessorImpl(productService);
    }

    @Bean
    public ProductSubCategoryServiceProcessor productSubCategoryServiceProcessor(ProductSubCategoryService productSubCategoryService) {
        return new ProductSubCategoryServiceProcessorImpl(productSubCategoryService);
    }

    @Bean
    public PurchaseOrderLineServiceProcessor purchaseOrderLineServiceProcessor(PurchaseOrderLineService purchaseOrderLineService) {
        return new PurchaseOrderLineServiceProcessorImpl(purchaseOrderLineService);
    }

    @Bean
    public PurchaseOrderServiceProcessor purchaseOrderServiceProcessor(PurchaseOrderService purchaseOrderService) {
        return new PurchaseOrderServiceProcessorImpl(purchaseOrderService);
    }

    @Bean
    public PurchaseReturnServiceProcessor purchaseReturnServiceProcessor(ObjectProvider<PurchaseReturnService> purchaseReturnServiceProvider) {
        PurchaseReturnService purchaseReturnService = purchaseReturnServiceProvider.getIfAvailable();
        if (purchaseReturnService == null) {
            throw new IllegalStateException("PurchaseReturnService bean is not available to create PurchaseReturnServiceProcessor");
        }
        return new PurchaseReturnServiceProcessorImpl(purchaseReturnService);
    }

    @Bean
    public PurchaseRequisitionServiceProcessor purchaseRequisitionServiceProcessor(PurchaseRequisitionService purchaseRequisitionService) {
        return new PurchaseRequisitionServiceProcessorImpl(purchaseRequisitionService);
    }

    @Bean
    public SalesOrderLineServiceProcessor salesOrderLineServiceProcessor() {
        return new SalesOrderLineServiceProcessorImpl();
    }

    @Bean
    public SalesOrderServiceProcessor salesOrderServiceProcessor(SalesOrderService salesOrderService) {
        return new SalesOrderServiceProcessorImpl(salesOrderService);
    }

    @Bean
    public SalesOrderDispatchServiceProcessor salesOrderDispatchServiceProcessor(
            SalesOrderDispatchService salesOrderDispatchService) {
        return new SalesOrderDispatchServiceProcessorImpl(salesOrderDispatchService);
    }

    @Bean
    public SalesReservationServiceProcessor salesReservationServiceProcessor(SalesReservationService salesReservationService) {
        return new SalesReservationServiceProcessorImpl(salesReservationService);
    }

    @Bean
    public StockAdjustmentServiceProcessor stockAdjustmentServiceProcessor(ObjectProvider<StockAdjustmentService> stockAdjustmentServiceProvider) {
        StockAdjustmentService stockAdjustmentService = stockAdjustmentServiceProvider.getIfAvailable();
        if (stockAdjustmentService == null) {
            throw new IllegalStateException("StockAdjustmentService bean is not available to create StockAdjustmentServiceProcessor");
        }
        return new StockAdjustmentServiceProcessorImpl(stockAdjustmentService);
    }

    @Bean
    public StockTransactionHistoryServiceProcessor stockTransactionHistoryServiceProcessor(StockTransactionHistoryService stockTransactionHistoryService) {
        return new StockTransactionHistoryServiceProcessorImpl(stockTransactionHistoryService);
    }

    @Bean
    public WarehouseLocationServiceProcessor warehouseLocationServiceProcessor(WarehouseLocationService warehouseLocationService) {
        return new WarehouseLocationServiceProcessorImpl(warehouseLocationService);
    }

    @Bean
    public projectlx.inventory.management.service.processor.api.LogisticsRouteStopServiceProcessor
    logisticsRouteStopServiceProcessor(
            projectlx.inventory.management.business.logic.api.LogisticsRouteStopService logisticsRouteStopService) {
        return new projectlx.inventory.management.service.processor.impl.LogisticsRouteStopServiceProcessorImpl(
                logisticsRouteStopService);
    }

    @Bean
    public projectlx.inventory.management.service.processor.api.InventoryIntegrationCredentialServiceProcessor
    inventoryIntegrationCredentialServiceProcessor(
            projectlx.inventory.management.business.logic.api.InventoryIntegrationCredentialService inventoryIntegrationCredentialService) {
        return new projectlx.inventory.management.service.processor.impl.InventoryIntegrationCredentialServiceProcessorImpl(
                inventoryIntegrationCredentialService);
    }

    @Bean
    public projectlx.inventory.management.service.processor.api.CrossDockDispatchServiceProcessor
    crossDockDispatchServiceProcessor(
            projectlx.inventory.management.business.logic.api.CrossDockDispatchService crossDockDispatchService) {
        return new projectlx.inventory.management.service.processor.impl.CrossDockDispatchServiceProcessorImpl(
                crossDockDispatchService);
    }
}
