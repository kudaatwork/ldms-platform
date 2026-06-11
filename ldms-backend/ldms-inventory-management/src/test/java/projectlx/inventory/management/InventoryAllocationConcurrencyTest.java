package projectlx.inventory.management;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import projectlx.inventory.management.business.logic.api.InventoryAllocationService;
import projectlx.inventory.management.business.logic.impl.InventoryAllocationServiceImpl;
import projectlx.inventory.management.model.*;
import projectlx.inventory.management.repository.*;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@ExtendWith(SpringExtension.class)
@DataJpaTest
class InventoryAllocationConcurrencyTest {

    private InventoryAllocationService inventoryAllocationService;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Autowired
    private InventoryReservationRepository inventoryReservationRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private WarehouseLocationRepository warehouseLocationRepository;

    @Autowired
    private ProductCategoryRepository productCategoryRepository;

    private static final ExecutorService EXEC = Executors.newFixedThreadPool(2);

    @AfterAll
    static void shutdown() {
        EXEC.shutdownNow();
    }

    @Test
    void parallelAllocationsContendOnSameItem() throws ExecutionException, InterruptedException {
        // Arrange: seed minimal data
        ProductCategory category = new ProductCategory();
        category.setName("Default");
        category.setDescription("Default Cat");
        productCategoryRepository.save(category);

        Product product = new Product();
        product.setName("Test Product");
        product.setDescription("Simple");
        product.setPrice(new BigDecimal("1.00"));
        product.setUnitOfMeasure(UnitOfMeasure.EACH);
        product.setProductCode("TP-001");
        product.setCategory(category);
        product.setSupplierId(1L);
        productRepository.save(product);

        WarehouseLocation wh = new WarehouseLocation();
        wh.setName("Main");
        wh.setDescription("Main WH");
        wh.setLocationId("LOC-1");
        wh.setSupplierId(1L);
        warehouseLocationRepository.save(wh);

        InventoryItem item = new InventoryItem();
        item.setProduct(product);
        item.setWarehouseLocation(wh);
        item.setSupplierId(1L);
        item.setCurrentStock(new BigDecimal("10"));
        item.setReservedQuantity(BigDecimal.ZERO);
        item.setTotalCost(BigDecimal.ZERO);
        item.setUnitCost(BigDecimal.ZERO);
        item.setAverageCost(BigDecimal.ZERO);
        item.setEntityStatus(EntityStatus.ACTIVE);
        inventoryItemRepository.save(item);

        // Two orders each trying to reserve 7
        SalesOrder so1 = buildOrder(product, new BigDecimal("7"), "SO-1");
        SalesOrder so2 = buildOrder(product, new BigDecimal("7"), "SO-2");

        Callable<InventoryAllocationServiceImpl.AllocationResult> task1 = () -> inventoryAllocationService.allocateInventory(so1, wh.getId());
        Callable<InventoryAllocationServiceImpl.AllocationResult> task2 = () -> inventoryAllocationService.allocateInventory(so2, wh.getId());

        // Act: run concurrently
        List<Future<InventoryAllocationServiceImpl.AllocationResult>> results = EXEC.invokeAll(Arrays.asList(task1, task2));
        InventoryAllocationServiceImpl.AllocationResult r1 = results.get(0).get();
        InventoryAllocationServiceImpl.AllocationResult r2 = results.get(1).get();

        // Assert: exactly one succeeded
        boolean exactlyOneSuccess = (r1.isSuccess() && !r2.isSuccess()) || (!r1.isSuccess() && r2.isSuccess());
        Assertions.assertTrue(exactlyOneSuccess, "Exactly one allocation should succeed under contention");

        // Reload item and check reserved quantity not over-allocated
        Optional<InventoryItem> reloadedOpt = inventoryItemRepository.findByProductIdAndWarehouseLocationIdAndEntityStatusNot(product.getId(), wh.getId(), EntityStatus.DELETED);
        Assertions.assertTrue(reloadedOpt.isPresent(), "Inventory item should exist");
        InventoryItem reloaded = reloadedOpt.get();

        // Reserved should be 7 (one order), never exceed current stock 10
        Assertions.assertEquals(new BigDecimal("7.0000"), reloaded.getReservedQuantity().setScale(4), "Reserved quantity should reflect single successful reservation");
        Assertions.assertTrue(reloaded.getReservedQuantity().compareTo(reloaded.getCurrentStock()) <= 0, "Reserved must not exceed current stock");

        // Also ensure there is exactly one reservation row persisted
        List<InventoryReservation> reservations = inventoryReservationRepository.findBySalesOrderId(so1.getId());
        reservations.addAll(inventoryReservationRepository.findBySalesOrderId(so2.getId()));
        long totalReservedQty = reservations.stream().map(InventoryReservation::getQuantity).map(q -> q == null ? BigDecimal.ZERO : q).reduce(BigDecimal.ZERO, BigDecimal::add).longValue();
        Assertions.assertTrue(totalReservedQty == 7L, "Total reserved quantity rows should sum to 7");
    }

    private SalesOrder buildOrder(Product product, BigDecimal qty, String soNumber) {
        SalesOrder so = new SalesOrder();
        so.setSalesOrderNumber(soNumber);
        so.setCustomerId(1L);
        so.setStatus(SalesOrderStatus.PENDING);
        so.setOrderDate(LocalDate.now());

        SalesOrderLine line = new SalesOrderLine();
        line.setSalesOrder(so);
        line.setProduct(product);
        line.setQuantity(qty);
        line.setUnitPrice(new BigDecimal("1.00"));
        line.setTotalPrice(qty);

        so.getSalesOrderLines().add(line);
        return so;
    }
}
