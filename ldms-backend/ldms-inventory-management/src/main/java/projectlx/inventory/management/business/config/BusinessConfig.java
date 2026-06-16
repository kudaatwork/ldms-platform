package projectlx.inventory.management.business.config;

import jakarta.persistence.EntityManager;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import projectlx.inventory.management.business.auditable.api.GoodsReceivedVoucherServiceAuditable;
import projectlx.inventory.management.business.auditable.api.InventoryItemServiceAuditable;
import projectlx.inventory.management.business.auditable.api.InventoryTransferServiceAuditable;
import projectlx.inventory.management.business.auditable.api.ProductCategoryServiceAuditable;
import projectlx.inventory.management.business.auditable.api.ProductDocumentServiceAuditable;
import projectlx.inventory.management.business.auditable.api.ProductServiceAuditable;
import projectlx.inventory.management.business.auditable.api.ProductSubCategoryServiceAuditable;
import projectlx.inventory.management.business.auditable.api.PurchaseOrderLineServiceAuditable;
import projectlx.inventory.management.business.auditable.api.PurchaseOrderServiceAuditable;
import projectlx.inventory.management.business.auditable.api.PurchaseRequisitionServiceAuditable;
import projectlx.inventory.management.business.auditable.api.PurchaseReturnServiceAuditable;
import projectlx.inventory.management.business.auditable.api.SalesOrderLineServiceAuditable;
import projectlx.inventory.management.business.auditable.api.SalesOrderServiceAuditable;
import projectlx.inventory.management.business.auditable.api.SalesReservationServiceAuditable;
import projectlx.inventory.management.business.auditable.api.StockAdjustmentServiceAuditable;
import projectlx.inventory.management.business.auditable.api.StockTransactionHistoryServiceAuditable;
import projectlx.inventory.management.business.auditable.api.WarehouseLocationServiceAuditable;
import projectlx.inventory.management.business.auditable.impl.GoodsReceivedVoucherServiceAuditableImpl;
import projectlx.inventory.management.business.auditable.impl.InventoryItemServiceAuditableImpl;
import projectlx.inventory.management.business.auditable.impl.PurchaseRequisitionServiceAuditableImpl;
import projectlx.inventory.management.business.auditable.impl.InventoryTransferServiceAuditableImpl;
import projectlx.inventory.management.business.auditable.impl.ProductCategoryServiceAuditableImpl;
import projectlx.inventory.management.business.auditable.impl.ProductDocumentServiceAuditableImpl;
import projectlx.inventory.management.business.auditable.impl.ProductServiceAuditableImpl;
import projectlx.inventory.management.business.auditable.impl.ProductSubCategoryServiceAuditableImpl;
import projectlx.inventory.management.business.auditable.impl.PurchaseOrderLineServiceAuditableImpl;
import projectlx.inventory.management.business.auditable.impl.PurchaseOrderServiceAuditableImpl;
import projectlx.inventory.management.business.auditable.impl.PurchaseReturnServiceAuditableImpl;
import projectlx.inventory.management.business.auditable.impl.SalesOrderLineServiceAuditableImpl;
import projectlx.inventory.management.business.auditable.impl.SalesOrderServiceAuditableImpl;
import projectlx.inventory.management.business.auditable.impl.SalesReservationServiceAuditableImpl;
import projectlx.inventory.management.business.auditable.impl.StockAdjustmentServiceAuditableImpl;
import projectlx.inventory.management.business.auditable.impl.StockTransactionHistoryServiceAuditableImpl;
import projectlx.inventory.management.business.auditable.impl.WarehouseLocationServiceAuditableImpl;
import projectlx.inventory.management.business.logic.api.AuditTrailService;
import projectlx.inventory.management.business.logic.api.ConcurrentInventoryHandler;
import projectlx.inventory.management.business.logic.api.GoodsReceiptProcessor;
import projectlx.inventory.management.business.logic.api.InventoryAllocationService;
import projectlx.inventory.management.business.logic.api.InventoryItemService;
import projectlx.inventory.management.business.logic.api.InventoryTransferService;
import projectlx.inventory.management.business.logic.api.ProductCategoryService;
import projectlx.inventory.management.business.logic.api.ProductDocumentService;
import projectlx.inventory.management.business.logic.api.ProductSubCategoryService;
import projectlx.inventory.management.business.logic.api.PurchaseOrderLineService;
import projectlx.inventory.management.business.logic.api.PurchaseOrderService;
import projectlx.inventory.management.business.logic.api.PurchaseRequisitionService;
import projectlx.inventory.management.business.logic.api.PurchaseReturnService;
import projectlx.inventory.management.business.logic.api.SalesOrderLineService;
import projectlx.inventory.management.business.logic.api.SalesOrderService;
import projectlx.inventory.management.business.logic.api.SalesReservationService;
import projectlx.inventory.management.business.logic.api.StockAdjustmentService;
import projectlx.inventory.management.business.logic.api.StockTransactionHistoryService;
import projectlx.inventory.management.business.logic.api.WarehouseLocationService;
import projectlx.inventory.management.business.logic.impl.AuditTrailServiceImpl;
import projectlx.inventory.management.business.logic.impl.ConcurrentInventoryHandlerImpl;
import projectlx.inventory.management.business.logic.impl.InventoryAllocationServiceImpl;
import projectlx.inventory.management.business.logic.impl.InventoryItemServiceImpl;
import projectlx.inventory.management.business.logic.impl.InventoryTransferServiceImpl;
import projectlx.inventory.management.business.logic.impl.ProductCategoryServiceImpl;
import projectlx.inventory.management.business.logic.impl.ProductDocumentServiceImpl;
import projectlx.inventory.management.business.logic.impl.ProductServiceImpl;
import projectlx.inventory.management.business.logic.impl.ProductSubCategoryServiceImpl;
import projectlx.inventory.management.business.logic.impl.PurchaseOrderLineServiceImpl;
import projectlx.inventory.management.business.logic.impl.PurchaseOrderServiceImpl;
import projectlx.inventory.management.business.logic.impl.PurchaseRequisitionServiceImpl;
import projectlx.inventory.management.business.logic.impl.PurchaseReturnServiceImpl;
import projectlx.inventory.management.business.logic.impl.SalesOrderLineServiceImpl;
import projectlx.inventory.management.business.logic.impl.SalesOrderServiceImpl;
import projectlx.inventory.management.business.logic.impl.SalesOrderDispatchServiceImpl;
import projectlx.inventory.management.business.logic.api.SalesOrderDispatchService;
import projectlx.inventory.management.business.logic.support.SalesOrderDispatchSupport;
import projectlx.inventory.management.business.logic.impl.SalesReservationServiceImpl;
import projectlx.inventory.management.business.logic.impl.StockAdjustmentServiceImpl;
import projectlx.inventory.management.business.logic.impl.StockTransactionHistoryServiceImpl;
import projectlx.inventory.management.business.logic.impl.WarehouseLocationServiceImpl;
import projectlx.inventory.management.business.validator.api.GoodsReceivedVoucherServiceValidator;
import projectlx.inventory.management.business.validator.api.InventoryItemServiceValidator;
import projectlx.inventory.management.business.validator.api.InventoryTransferServiceValidator;
import projectlx.inventory.management.business.validator.api.ProductCategoryServiceValidator;
import projectlx.inventory.management.business.validator.api.ProductDocumentServiceValidator;
import projectlx.inventory.management.business.validator.api.ProductSubCategoryServiceValidator;
import projectlx.inventory.management.business.validator.api.PurchaseOrderLineServiceValidator;
import projectlx.inventory.management.business.validator.api.PurchaseOrderServiceValidator;
import projectlx.inventory.management.business.validator.api.PurchaseRequisitionServiceValidator;
import projectlx.inventory.management.business.validator.api.PurchaseReturnServiceValidator;
import projectlx.inventory.management.business.validator.api.SalesOrderLineServiceValidator;
import projectlx.inventory.management.business.validator.api.SalesOrderServiceValidator;
import projectlx.inventory.management.business.validator.api.SalesReservationServiceValidator;
import projectlx.inventory.management.business.validator.api.StockAdjustmentServiceValidator;
import projectlx.inventory.management.business.validator.api.StockTransactionHistoryServiceValidator;
import projectlx.inventory.management.business.validator.api.WarehouseLocationServiceValidator;
import projectlx.inventory.management.business.validator.impl.GoodsReceivedVoucherServiceValidatorImpl;
import projectlx.inventory.management.business.validator.impl.InventoryItemServiceValidatorImpl;
import projectlx.inventory.management.business.validator.impl.InventoryTransferServiceValidatorImpl;
import projectlx.inventory.management.business.validator.impl.ProductCategoryServiceValidatorImpl;
import projectlx.inventory.management.business.validator.impl.ProductDocumentServiceValidatorImpl;
import projectlx.inventory.management.business.validator.impl.ProductServiceValidatorImpl;
import projectlx.inventory.management.business.validator.impl.ProductSubCategoryServiceValidatorImpl;
import projectlx.inventory.management.business.validator.impl.PurchaseOrderLineServiceValidatorImpl;
import projectlx.inventory.management.business.validator.impl.PurchaseOrderServiceValidatorImpl;
import projectlx.inventory.management.business.validator.impl.PurchaseRequisitionServiceValidatorImpl;
import projectlx.inventory.management.business.validator.impl.PurchaseReturnServiceValidatorImpl;
import projectlx.inventory.management.business.validator.impl.SalesOrderLineServiceValidatorImpl;
import projectlx.inventory.management.business.validator.impl.SalesOrderServiceValidatorImpl;
import projectlx.inventory.management.business.validator.impl.SalesReservationServiceValidatorImpl;
import projectlx.inventory.management.business.validator.impl.StockAdjustmentServiceValidatorImpl;
import projectlx.inventory.management.business.validator.impl.StockTransactionHistoryServiceValidatorImpl;
import projectlx.inventory.management.business.validator.impl.WarehouseLocationServiceValidatorImpl;
import projectlx.inventory.management.clients.LocationsServiceClient;
import projectlx.inventory.management.repository.GoodsReceivedVoucherRepository;
import projectlx.inventory.management.repository.InventoryItemRepository;
import projectlx.inventory.management.repository.InventoryTransferRepository;
import projectlx.inventory.management.repository.ProductCategoryRepository;
import projectlx.inventory.management.repository.ProductDocumentRepository;
import projectlx.inventory.management.repository.ProductRepository;
import projectlx.inventory.management.repository.ProductSubCategoryRepository;
import projectlx.inventory.management.repository.PurchaseOrderLineRepository;
import projectlx.inventory.management.repository.PurchaseOrderRepository;
import projectlx.inventory.management.repository.PurchaseRequisitionAmendmentRepository;
import projectlx.inventory.management.repository.PurchaseRequisitionLineRepository;
import projectlx.inventory.management.repository.PurchaseRequisitionRepository;
import projectlx.inventory.management.repository.PurchaseReturnRepository;
import projectlx.inventory.management.repository.SalesOrderLineRepository;
import projectlx.inventory.management.repository.SalesOrderRepository;
import projectlx.inventory.management.repository.SalesReservationRepository;
import projectlx.inventory.management.repository.StockAdjustmentRepository;
import projectlx.inventory.management.repository.StockTransactionHistoryRepository;
import projectlx.inventory.management.business.logic.support.BranchAllocationSupport;
import projectlx.inventory.management.business.logic.support.WarehouseAccessSupport;
import projectlx.inventory.management.business.logic.support.WarehouseSharingSupport;
import projectlx.inventory.management.repository.WarehouseLocationRepository;
import projectlx.inventory.management.repository.WarehouseOrganizationAccessRepository;
import projectlx.inventory.management.repository.InventoryReservationRepository;
import projectlx.inventory.management.utils.CostCalculator;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.inventory.management.business.logic.api.ProductService;
import projectlx.inventory.management.clients.FileUploadServiceClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import projectlx.inventory.management.business.validator.api.ProductServiceValidator;
import projectlx.inventory.management.business.logic.api.OutboxService;
import projectlx.inventory.management.business.logic.impl.OutboxServiceImpl;
import projectlx.inventory.management.repository.OutboxEventRepository;

@EnableAsync
@Configuration
@EnableFeignClients(basePackages = "projectlx.inventory.management.clients")
@EnableScheduling
public class BusinessConfig {

    // ======= Services =======
    @Bean
    public ProductCategoryService productCategoryService(
            ProductCategoryServiceValidator productCategoryServiceValidator,
            MessageService messageService,
            ModelMapper modelMapper,
            ProductCategoryRepository productCategoryRepository,
            ProductCategoryServiceAuditable productCategoryServiceAuditable
    ) {
        return new ProductCategoryServiceImpl(
                productCategoryServiceValidator,
                messageService,
                modelMapper,
                productCategoryRepository,
                productCategoryServiceAuditable
        );
    }

    @Bean
    public ProductSubCategoryService productSubCategoryService(
            ProductSubCategoryServiceValidator validator,
            MessageService messageService,
            ModelMapper modelMapper,
            ProductSubCategoryRepository repository,
            ProductSubCategoryServiceAuditable auditable,
            ProductCategoryRepository productCategoryRepository
    ) {
        return new ProductSubCategoryServiceImpl(
                validator,
                messageService,
                modelMapper,
                repository,
                auditable,
                productCategoryRepository
        );
    }

    @Bean
    public WarehouseLocationService warehouseLocationService(
            WarehouseLocationServiceValidator validator,
            MessageService messageService,
            ModelMapper modelMapper,
            WarehouseLocationRepository repository,
            WarehouseLocationServiceAuditable auditable,
            LocationsServiceClient locationsServiceClient,
            BranchAllocationSupport branchAllocationSupport,
            projectlx.inventory.management.clients.UserManagementServiceClient userManagementServiceClient,
            WarehouseAccessSupport warehouseAccessSupport,
            WarehouseSharingSupport warehouseSharingSupport,
            WarehouseOrganizationAccessRepository warehouseOrganizationAccessRepository
    ) {
        return new WarehouseLocationServiceImpl(
                validator,
                messageService,
                modelMapper,
                repository,
                auditable,
                locationsServiceClient,
                branchAllocationSupport,
                userManagementServiceClient,
                warehouseAccessSupport,
                warehouseSharingSupport,
                warehouseOrganizationAccessRepository
        );
    }

    @Bean
    public ProductService productService(
            ProductServiceValidator validator,
            MessageService messageService,
            ModelMapper modelMapper,
            ProductRepository productRepository,
            ProductCategoryRepository productCategoryRepository,
            ProductSubCategoryRepository productSubCategoryRepository,
            ProductServiceAuditable auditable,
            FileUploadServiceClient fileUploadServiceClient,
            ObjectMapper objectMapper
    ) {
        return new ProductServiceImpl(
                validator,
                messageService,
                modelMapper,
                productRepository,
                productCategoryRepository,
                productSubCategoryRepository,
                auditable,
                fileUploadServiceClient,
                objectMapper
        );
    }

    @Bean
    public InventoryAllocationService inventoryAllocationService(InventoryItemRepository inventoryItemRepository,
                                                                 InventoryReservationRepository inventoryReservationRepository) {
        return new InventoryAllocationServiceImpl(inventoryItemRepository, inventoryReservationRepository);
    }

    @Bean
    public CostCalculator costCalculator() {
        return new CostCalculator();
    }

    @Bean
    public ConcurrentInventoryHandler concurrentInventoryHandler(InventoryItemRepository inventoryItemRepository, CostCalculator costCalculator) {
        return new ConcurrentInventoryHandlerImpl(inventoryItemRepository, costCalculator);
    }

    @Bean
    public PurchaseOrderLineService purchaseOrderLineService(
            PurchaseOrderLineRepository purchaseOrderLineRepository,
            PurchaseOrderRepository purchaseOrderRepository,
            ProductRepository productRepository,
            PurchaseOrderLineServiceValidator validator,
            PurchaseOrderLineServiceAuditable auditable,
            ModelMapper modelMapper,
            MessageService messageService
    ) {
        return new PurchaseOrderLineServiceImpl(
                purchaseOrderLineRepository,
                purchaseOrderRepository,
                productRepository,
                validator,
                auditable,
                modelMapper,
                messageService
        );
    }

    @Bean
    public PurchaseOrderService purchaseOrderService(
            PurchaseOrderRepository purchaseOrderRepository,
            ProductRepository productRepository,
            PurchaseOrderServiceAuditable purchaseOrderServiceAuditable,
            ModelMapper modelMapper,
            PurchaseOrderServiceValidator validator,
            MessageService messageService,
            PurchaseOrderLineService purchaseOrderLineService,
            InventoryItemService inventoryItemService,
            GoodsReceivedVoucherRepository goodsReceivedVoucherRepository,
            GoodsReceivedVoucherServiceAuditable goodsReceivedVoucherServiceAuditable,
            StockTransactionHistoryRepository stockTransactionHistoryRepository,
            RabbitTemplate rabbitTemplate,
            projectlx.inventory.management.clients.OrganizationServiceClient organizationServiceClient,
            projectlx.inventory.management.clients.UserManagementServiceClient userManagementServiceClient,
            projectlx.inventory.management.business.logic.api.IdempotencyService idempotencyService,
            ApplicationEventPublisher applicationEventPublisher,
            GoodsReceiptProcessor goodsReceiptProcessor,
            projectlx.inventory.management.business.logic.support.OrganizationFunctionalCurrencySupport organizationFunctionalCurrencySupport,
            projectlx.inventory.management.business.logic.support.TransactionCurrencyConversionSupport transactionCurrencyConversionSupport
    ) {
        return new PurchaseOrderServiceImpl(
                purchaseOrderRepository,
                productRepository,
                purchaseOrderServiceAuditable,
                modelMapper,
                validator,
                messageService,
                purchaseOrderLineService,
                inventoryItemService,
                goodsReceivedVoucherRepository,
                goodsReceivedVoucherServiceAuditable,
                stockTransactionHistoryRepository,
                rabbitTemplate,
                organizationServiceClient,
                userManagementServiceClient,
                idempotencyService,
                applicationEventPublisher,
                goodsReceiptProcessor,
                organizationFunctionalCurrencySupport,
                transactionCurrencyConversionSupport
        );
    }

    @Bean
    public PurchaseRequisitionService purchaseRequisitionService(
            PurchaseRequisitionRepository purchaseRequisitionRepository,
            PurchaseRequisitionLineRepository purchaseRequisitionLineRepository,
            PurchaseRequisitionAmendmentRepository purchaseRequisitionAmendmentRepository,
            ProductRepository productRepository,
            PurchaseOrderRepository purchaseOrderRepository,
            PurchaseOrderLineRepository purchaseOrderLineRepository,
            WarehouseLocationRepository warehouseLocationRepository,
            PurchaseRequisitionServiceAuditable purchaseRequisitionServiceAuditable,
            PurchaseRequisitionServiceValidator purchaseRequisitionServiceValidator,
            ModelMapper modelMapper,
            MessageService messageService,
            projectlx.inventory.management.business.logic.support.OrganizationFunctionalCurrencySupport organizationFunctionalCurrencySupport,
            projectlx.inventory.management.business.logic.support.ProcurementApprovalStageResolver procurementApprovalStageResolver
    ) {
        return new PurchaseRequisitionServiceImpl(
                purchaseRequisitionRepository,
                purchaseRequisitionLineRepository,
                purchaseRequisitionAmendmentRepository,
                productRepository,
                purchaseOrderRepository,
                purchaseOrderLineRepository,
                warehouseLocationRepository,
                purchaseRequisitionServiceAuditable,
                purchaseRequisitionServiceValidator,
                modelMapper,
                messageService,
                organizationFunctionalCurrencySupport,
                procurementApprovalStageResolver
        );
    }

    @Bean
    public InventoryItemService inventoryItemService(
            InventoryItemRepository inventoryItemRepository,
            ProductRepository productRepository,
            WarehouseLocationRepository warehouseLocationRepository,
            StockTransactionHistoryServiceAuditable stockTransactionHistoryServiceAuditable,
            InventoryItemServiceAuditable inventoryItemServiceAuditable,
            ConcurrentInventoryHandler concurrentInventoryHandler,
            ModelMapper modelMapper,
            MessageService messageService,
            InventoryItemServiceValidator validator,
            RabbitTemplate rabbitTemplate,
            projectlx.inventory.management.clients.OrganizationServiceClient organizationServiceClient,
            projectlx.inventory.management.clients.UserManagementServiceClient userManagementServiceClient,
            ObjectMapper objectMapper,
            EntityManager entityManager,
            @Lazy InventoryItemService inventoryItemServiceSelf
    ) {
        return new InventoryItemServiceImpl(
                inventoryItemRepository,
                productRepository,
                warehouseLocationRepository,
                stockTransactionHistoryServiceAuditable,
                inventoryItemServiceAuditable,
                concurrentInventoryHandler,
                modelMapper,
                messageService,
                validator,
                rabbitTemplate,
                organizationServiceClient,
                userManagementServiceClient,
                objectMapper,
                entityManager,
                inventoryItemServiceSelf
        );
    }

    @Bean
    public PurchaseReturnService purchaseReturnService(
            PurchaseReturnRepository purchaseReturnRepository,
            PurchaseOrderRepository purchaseOrderRepository,
            WarehouseLocationRepository warehouseLocationRepository,
            InventoryItemRepository inventoryItemRepository,
            PurchaseReturnServiceAuditable purchaseReturnServiceAuditable,
            InventoryItemService inventoryItemService,
            ModelMapper modelMapper,
            MessageService messageService,
            PurchaseReturnServiceValidator validator,
            RabbitTemplate rabbitTemplate
    ) {
        return new PurchaseReturnServiceImpl(
                purchaseReturnRepository,
                purchaseOrderRepository,
                warehouseLocationRepository,
                inventoryItemRepository,
                purchaseReturnServiceAuditable,
                inventoryItemService,
                modelMapper,
                messageService,
                validator,
                rabbitTemplate
        );
    }

    @Bean
    public StockAdjustmentService stockAdjustmentService(
            StockAdjustmentRepository stockAdjustmentRepository,
            InventoryItemRepository inventoryItemRepository,
            StockAdjustmentServiceAuditable auditable,
            StockAdjustmentServiceValidator validator,
            ModelMapper modelMapper,
            MessageService messageService,
            InventoryItemService inventoryItemService,
            StockTransactionHistoryServiceAuditable stockTransactionHistoryServiceAuditable,
            RabbitTemplate rabbitTemplate
    ) {
        return new StockAdjustmentServiceImpl(
                stockAdjustmentRepository,
                inventoryItemRepository,
                auditable,
                validator,
                modelMapper,
                messageService,
                inventoryItemService,
                stockTransactionHistoryServiceAuditable,
                rabbitTemplate
        );
    }

    @Bean
    public StockTransactionHistoryService stockTransactionHistoryService(
            StockTransactionHistoryRepository repository,
            InventoryItemRepository inventoryItemRepository,
            WarehouseLocationRepository warehouseLocationRepository,
            StockTransactionHistoryServiceAuditable auditable,
            StockTransactionHistoryServiceValidator validator,
            ModelMapper modelMapper,
            MessageService messageService
    ) {
        return new StockTransactionHistoryServiceImpl(
                repository,
                inventoryItemRepository,
                warehouseLocationRepository,
                auditable,
                validator,
                modelMapper,
                messageService
        );
    }

    @Bean
    public InventoryTransferService inventoryTransferService(
            InventoryTransferRepository repository,
            ProductRepository productRepository,
            WarehouseLocationRepository warehouseLocationRepository,
            InventoryTransferServiceAuditable auditable,
            InventoryItemService inventoryItemService,
            InventoryItemRepository inventoryItemRepository,
            StockTransactionHistoryRepository stockTransactionHistoryRepository,
            InventoryTransferServiceValidator inventoryTransferServiceValidator,
            ModelMapper modelMapper,
            MessageService messageService,
            RabbitTemplate rabbitTemplate,
            projectlx.inventory.management.business.logic.api.IdempotencyService idempotencyService,
            projectlx.inventory.management.clients.UserManagementServiceClient userManagementServiceClient,
            projectlx.inventory.management.business.logic.support.ProcurementApproverSupport procurementApproverSupport,
            projectlx.inventory.management.business.logic.support.TransferDispatchSupport transferDispatchSupport,
            projectlx.inventory.management.business.logic.support.TransitWarehouseSupport transitWarehouseSupport,
            projectlx.inventory.management.business.logic.support.StockTransferSupport stockTransferSupport,
            GoodsReceivedVoucherServiceAuditable goodsReceivedVoucherServiceAuditable,
            GoodsReceivedVoucherRepository goodsReceivedVoucherRepository
    ) {
        return new InventoryTransferServiceImpl(
                repository,
                productRepository,
                warehouseLocationRepository,
                auditable,
                inventoryItemService,
                inventoryItemRepository,
                stockTransactionHistoryRepository,
                inventoryTransferServiceValidator,
                modelMapper,
                messageService,
                rabbitTemplate,
                idempotencyService,
                userManagementServiceClient,
                procurementApproverSupport,
                transferDispatchSupport,
                transitWarehouseSupport,
                stockTransferSupport,
                goodsReceivedVoucherServiceAuditable,
                goodsReceivedVoucherRepository
        );
    }

    @Bean
    public ProductDocumentService productDocumentService(
            ProductDocumentServiceValidator validator,
            MessageService messageService,
            ModelMapper modelMapper,
            ProductRepository productRepository,
            ProductDocumentRepository productDocumentRepository,
            ProductDocumentServiceAuditable auditable,
            FileUploadServiceClient fileUploadServiceClient,
            ObjectMapper objectMapper
    ) {
        return new ProductDocumentServiceImpl(
                validator,
                messageService,
                modelMapper,
                productRepository,
                productDocumentRepository,
                auditable,
                fileUploadServiceClient,
                objectMapper
        );
    }


    // ======= Auditable implementations =======
    @Bean
    @Primary
    public InventoryItemServiceAuditable inventoryItemServiceAuditable(InventoryItemRepository inventoryItemRepository) {
        return new InventoryItemServiceAuditableImpl(inventoryItemRepository);
    }

    @Bean
    @Primary
    public InventoryTransferServiceAuditable inventoryTransferServiceAuditable(InventoryTransferRepository inventoryTransferRepository) {
        return new InventoryTransferServiceAuditableImpl(inventoryTransferRepository);
    }

    @Bean
    @Primary
    public ProductDocumentServiceAuditable productDocumentServiceAuditable(ProductDocumentRepository productDocumentRepository) {
        return new ProductDocumentServiceAuditableImpl(productDocumentRepository);
    }

    @Bean
    @Primary
    public PurchaseOrderLineServiceAuditable purchaseOrderLineServiceAuditable(PurchaseOrderLineRepository purchaseOrderLineRepository) {
        return new PurchaseOrderLineServiceAuditableImpl(purchaseOrderLineRepository);
    }

    @Bean
    @Primary
    public PurchaseOrderServiceAuditable purchaseOrderServiceAuditable(PurchaseOrderRepository purchaseOrderRepository) {
        return new PurchaseOrderServiceAuditableImpl(purchaseOrderRepository);
    }

    @Bean
    @Primary
    public SalesOrderLineServiceAuditable salesOrderLineServiceAuditable(SalesOrderLineRepository salesOrderLineRepository) {
        return new SalesOrderLineServiceAuditableImpl(salesOrderLineRepository);
    }

    @Bean
    @Primary
    public SalesOrderServiceAuditable salesOrderServiceAuditable(SalesOrderRepository salesOrderRepository) {
        return new SalesOrderServiceAuditableImpl(salesOrderRepository);
    }

    @Bean
    @Primary
    public SalesReservationServiceAuditable salesReservationServiceAuditable(SalesReservationRepository salesReservationRepository) {
        return new SalesReservationServiceAuditableImpl(salesReservationRepository);
    }

    @Bean
    @Primary
    public StockAdjustmentServiceAuditable stockAdjustmentServiceAuditable(StockAdjustmentRepository stockAdjustmentRepository) {
        return new StockAdjustmentServiceAuditableImpl(stockAdjustmentRepository);
    }

    @Bean
    @Primary
    public StockTransactionHistoryServiceAuditable stockTransactionHistoryServiceAuditable(StockTransactionHistoryRepository stockTransactionHistoryRepository) {
        return new StockTransactionHistoryServiceAuditableImpl(stockTransactionHistoryRepository);
    }

    @Bean
    @Primary
    public GoodsReceivedVoucherServiceAuditable goodsReceivedVoucherServiceAuditable(GoodsReceivedVoucherRepository goodsReceivedVoucherRepository) {
        return new GoodsReceivedVoucherServiceAuditableImpl(goodsReceivedVoucherRepository);
    }

    @Bean
    @Primary
    public ProductCategoryServiceAuditable productCategoryServiceAuditable(ProductCategoryRepository productCategoryRepository) {
        return new ProductCategoryServiceAuditableImpl(productCategoryRepository);
    }

    @Bean
    @Primary
    public ProductSubCategoryServiceAuditable productSubCategoryServiceAuditable(ProductSubCategoryRepository productSubCategoryRepository) {
        return new ProductSubCategoryServiceAuditableImpl(productSubCategoryRepository);
    }

    @Bean
    @Primary
    public PurchaseReturnServiceAuditable purchaseReturnServiceAuditable(PurchaseReturnRepository purchaseReturnRepository) {
        return new PurchaseReturnServiceAuditableImpl(purchaseReturnRepository);
    }

    @Bean
    @Primary
    public PurchaseRequisitionServiceAuditable purchaseRequisitionServiceAuditable(
            PurchaseRequisitionRepository purchaseRequisitionRepository,
            PurchaseRequisitionAmendmentRepository purchaseRequisitionAmendmentRepository
    ) {
        return new PurchaseRequisitionServiceAuditableImpl(
                purchaseRequisitionRepository,
                purchaseRequisitionAmendmentRepository
        );
    }

    // ======= Validators =======
    @Bean
    @Primary
    public InventoryItemServiceValidator inventoryItemServiceValidator(MessageService messageService) {
        return new InventoryItemServiceValidatorImpl(messageService);
    }

    @Bean
    @Primary
    public InventoryTransferServiceValidator inventoryTransferServiceValidator(MessageService messageService) {
        return new InventoryTransferServiceValidatorImpl(messageService);
    }

    @Bean
    @Primary
    public ProductCategoryServiceValidator productCategoryServiceValidator(MessageService messageService) {
        return new ProductCategoryServiceValidatorImpl(messageService);
    }

    @Bean
    @Primary
    public ProductDocumentServiceValidator productDocumentServiceValidator(MessageService messageService) {
        return new ProductDocumentServiceValidatorImpl(messageService);
    }

    @Bean
    @Primary
    public ProductSubCategoryServiceValidator productSubCategoryServiceValidator(MessageService messageService) {
        return new ProductSubCategoryServiceValidatorImpl(messageService);
    }

    @Bean
    @Primary
    public PurchaseOrderLineServiceValidator purchaseOrderLineServiceValidator(MessageService messageService, ProductRepository productRepository) {
        return new PurchaseOrderLineServiceValidatorImpl(productRepository, messageService);
    }

    @Bean
    @Primary
    public PurchaseOrderServiceValidator purchaseOrderServiceValidator(MessageService messageService,
                                                                       ProductRepository productRepository,
                                                                       WarehouseLocationRepository warehouseLocationRepository) {
        return new PurchaseOrderServiceValidatorImpl(productRepository, warehouseLocationRepository, messageService);
    }

    @Bean
    @Primary
    public PurchaseReturnServiceValidator purchaseReturnServiceValidator(MessageService messageService) {
        return new PurchaseReturnServiceValidatorImpl(messageService);
    }

    @Bean
    @Primary
    public SalesOrderLineServiceValidator salesOrderLineServiceValidator(MessageService messageService) {
        return new SalesOrderLineServiceValidatorImpl(messageService);
    }

    @Bean
    @Primary
    public SalesOrderServiceValidator salesOrderServiceValidator(MessageService messageService) {
        return new SalesOrderServiceValidatorImpl(messageService);
    }

    @Bean
    @Primary
    public SalesReservationServiceValidator salesReservationServiceValidator(MessageService messageService) {
        return new SalesReservationServiceValidatorImpl(messageService);
    }

    @Bean
    @Primary
    public StockAdjustmentServiceValidator stockAdjustmentServiceValidator(MessageService messageService) {
        return new StockAdjustmentServiceValidatorImpl(messageService);
    }

    @Bean
    @Primary
    public StockTransactionHistoryServiceValidator stockTransactionHistoryServiceValidator(MessageService messageService) {
        return new StockTransactionHistoryServiceValidatorImpl(messageService);
    }

    @Bean
    @Primary
    public WarehouseLocationServiceValidator warehouseLocationServiceValidator(MessageService messageService) {
        return new WarehouseLocationServiceValidatorImpl(messageService);
    }

    @Bean
    @Primary
    public ProductServiceValidator productServiceValidator(MessageService messageService) {
        return new ProductServiceValidatorImpl(messageService);
    }

    @Bean
    @Primary
    public PurchaseRequisitionServiceValidator purchaseRequisitionServiceValidator(
            MessageService messageService,
            ProductRepository productRepository,
            WarehouseLocationRepository warehouseLocationRepository
    ) {
        return new PurchaseRequisitionServiceValidatorImpl(
                messageService,
                productRepository,
                warehouseLocationRepository
        );
    }

    @Bean
    @Primary
    public GoodsReceivedVoucherServiceValidator goodsReceivedVoucherServiceValidator(MessageService messageService) {
        return new GoodsReceivedVoucherServiceValidatorImpl(messageService);
    }

    @Bean
    public OutboxService outboxService(OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        return new OutboxServiceImpl(outboxEventRepository, objectMapper);
    }

    @Bean
    public SalesOrderService salesOrderService(
            SalesOrderRepository salesOrderRepository,
            ProductRepository productRepository,
            WarehouseLocationRepository warehouseLocationRepository,
            InventoryItemRepository inventoryItemRepository,
            SalesOrderServiceAuditable salesOrderServiceAuditable,
            InventoryItemService inventoryItemService,
            InventoryAllocationService inventoryAllocationService,
            projectlx.inventory.management.business.logic.api.SalesOrderStatusManager salesOrderStatusManager,
            SalesOrderServiceValidator validator,
            ModelMapper modelMapper,
            MessageService messageService,
            RabbitTemplate rabbitTemplate,
            projectlx.inventory.management.clients.OrganizationServiceClient organizationServiceClient,
            projectlx.inventory.management.clients.UserManagementServiceClient userManagementServiceClient,
            projectlx.inventory.management.business.logic.api.IdempotencyService idempotencyService,
            InventoryReservationRepository inventoryReservationRepository
    ) {
        return new SalesOrderServiceImpl(
                salesOrderRepository,
                productRepository,
                warehouseLocationRepository,
                inventoryItemRepository,
                salesOrderServiceAuditable,
                inventoryItemService,
                inventoryAllocationService,
                salesOrderStatusManager,
                validator,
                modelMapper,
                messageService,
                rabbitTemplate,
                organizationServiceClient,
                userManagementServiceClient,
                idempotencyService,
                inventoryReservationRepository
        );
    }

    @Bean
    public SalesOrderDispatchService salesOrderDispatchService(
            SalesOrderRepository salesOrderRepository,
            PurchaseOrderRepository purchaseOrderRepository,
            WarehouseLocationRepository warehouseLocationRepository,
            InventoryItemRepository inventoryItemRepository,
            InventoryReservationRepository inventoryReservationRepository,
            GoodsReceivedVoucherServiceAuditable goodsReceivedVoucherServiceAuditable,
            projectlx.inventory.management.business.logic.api.SalesOrderStatusManager salesOrderStatusManager,
            SalesOrderDispatchSupport salesOrderDispatchSupport,
            projectlx.inventory.management.business.logic.support.StockTransferSupport stockTransferSupport,
            projectlx.inventory.management.business.logic.support.TransitWarehouseSupport transitWarehouseSupport,
            PurchaseOrderLineService purchaseOrderLineService,
            projectlx.inventory.management.business.logic.api.IdempotencyService idempotencyService,
            RabbitTemplate rabbitTemplate,
            MessageService messageService,
            ModelMapper modelMapper) {
        return new SalesOrderDispatchServiceImpl(
                salesOrderRepository,
                purchaseOrderRepository,
                warehouseLocationRepository,
                inventoryItemRepository,
                inventoryReservationRepository,
                goodsReceivedVoucherServiceAuditable,
                salesOrderStatusManager,
                salesOrderDispatchSupport,
                stockTransferSupport,
                transitWarehouseSupport,
                purchaseOrderLineService,
                idempotencyService,
                rabbitTemplate,
                messageService,
                modelMapper);
    }

    @Bean
    public SalesReservationService salesReservationService(
            SalesReservationRepository salesReservationRepository,
            ProductRepository productRepository,
            WarehouseLocationRepository warehouseLocationRepository,
            InventoryItemRepository inventoryItemRepository,
            SalesReservationServiceAuditable salesReservationServiceAuditable,
            InventoryItemService inventoryItemService,
            SalesReservationServiceValidator validator,
            ModelMapper modelMapper,
            MessageService messageService
    ) {
        return new SalesReservationServiceImpl(
                salesReservationRepository,
                productRepository,
                warehouseLocationRepository,
                inventoryItemRepository,
                salesReservationServiceAuditable,
                inventoryItemService,
                validator,
                modelMapper,
                messageService
        );
    }

}
