package projectlx.inventory.management.model;

import jakarta.persistence.metamodel.ListAttribute;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import javax.annotation.processing.Generated;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(InventoryItem.class)
public abstract class InventoryItem_ {

    public static volatile SingularAttribute<InventoryItem, Long> id;
    public static volatile SingularAttribute<InventoryItem, Product> product;
    public static volatile SingularAttribute<InventoryItem, Long> supplierId;
    public static volatile SingularAttribute<InventoryItem, WarehouseLocation> warehouseLocation;
    public static volatile SingularAttribute<InventoryItem, BigDecimal> quantity;
    public static volatile SingularAttribute<InventoryItem, BigDecimal> currentStock;
    public static volatile SingularAttribute<InventoryItem, BigDecimal> reservedQuantity;
    public static volatile SingularAttribute<InventoryItem, BigDecimal> totalCost;
    public static volatile SingularAttribute<InventoryItem, BigDecimal> averageCost;
    public static volatile SingularAttribute<InventoryItem, BigDecimal> lastPurchaseCost;
    public static volatile SingularAttribute<InventoryItem, BigDecimal> unitCost;
    public static volatile SingularAttribute<InventoryItem, BigDecimal> minStockLevel;
    public static volatile SingularAttribute<InventoryItem, BigDecimal> reorderQuantity;
    public static volatile SingularAttribute<InventoryItem, String> batchLot;
    public static volatile SingularAttribute<InventoryItem, String> serialNumber;
    public static volatile ListAttribute<InventoryItem, StockTransactionHistory> transactions;
    public static volatile SingularAttribute<InventoryItem, Long> createdByUserId;
    public static volatile SingularAttribute<InventoryItem, Long> updatedByUserId;
    public static volatile SingularAttribute<InventoryItem, LocalDateTime> createdAt;
    public static volatile SingularAttribute<InventoryItem, LocalDateTime> updatedAt;
    public static volatile SingularAttribute<InventoryItem, LocalDate> expiresAt;
    public static volatile SingularAttribute<InventoryItem, EntityStatus> entityStatus;

}