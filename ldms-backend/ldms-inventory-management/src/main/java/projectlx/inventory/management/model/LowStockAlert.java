package projectlx.inventory.management.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "low_stock_alert")
@Data
public class LowStockAlert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "inventory_item_id", nullable = false)
    private Long inventoryItemId;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "product_name")
    private String productName;

    @Column(name = "warehouse_location_id")
    private Long warehouseLocationId;

    @Column(name = "current_quantity")
    private BigDecimal currentStock;

    @Column(name = "alert_threshold")
    private BigDecimal minStockLevel;

    @Column(name = "reorder_quantity")
    private BigDecimal reorderQuantity;

    @Column(name = "alert_sent_at")
    private LocalDateTime alertDate;

    @Column(name = "severity")
    private String severity;
}
