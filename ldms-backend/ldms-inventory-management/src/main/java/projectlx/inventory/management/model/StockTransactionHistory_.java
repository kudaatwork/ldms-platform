package projectlx.inventory.management.model;

import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import javax.annotation.processing.Generated;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(StockTransactionHistory.class)
public class StockTransactionHistory_ {
    public static volatile SingularAttribute<StockTransactionHistory, Long> id;
    public static volatile SingularAttribute<StockTransactionHistory, InventoryItem> inventoryItem;
    public static volatile SingularAttribute<StockTransactionHistory, TransactionType> transactionType;
    public static volatile SingularAttribute<StockTransactionHistory, BigDecimal> quantityChange;
    public static volatile SingularAttribute<StockTransactionHistory, LocalDateTime> timestamp;
    public static volatile SingularAttribute<StockTransactionHistory, WarehouseLocation> warehouseLocation;
    public static volatile SingularAttribute<StockTransactionHistory, Long> performedByUserId;
    public static volatile SingularAttribute<StockTransactionHistory, Long> referenceDocumentId;
    public static volatile SingularAttribute<StockTransactionHistory, String> referenceDocumentType;
    public static volatile SingularAttribute<StockTransactionHistory, String> reason;
    public static volatile SingularAttribute<StockTransactionHistory, LocalDateTime> createdAt;
    public static volatile SingularAttribute<StockTransactionHistory, LocalDateTime> updatedAt;
    public static volatile SingularAttribute<StockTransactionHistory, EntityStatus> entityStatus;
}
