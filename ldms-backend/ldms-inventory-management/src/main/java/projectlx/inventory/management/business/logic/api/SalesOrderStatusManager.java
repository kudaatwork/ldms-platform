package projectlx.inventory.management.business.logic.api;

import projectlx.inventory.management.model.SalesOrder;
import projectlx.inventory.management.model.SalesOrderStatus;
import java.util.Locale;

public interface SalesOrderStatusManager {
    // New API as per requirement
    void transition(SalesOrder order, SalesOrderStatus targetStatus,
                    Long warehouseId, String username, Locale locale);
    boolean canTransition(SalesOrderStatus current, SalesOrderStatus target);

    // Backward-compatible methods (to be removed later)
    @Deprecated
    default void updateOrderStatus(SalesOrder order, SalesOrderStatus newStatus,
                                   Long warehouseId, String username, Locale locale) {
        transition(order, newStatus, warehouseId, username, locale);
    }

    @Deprecated
    default boolean isValidStatusTransition(SalesOrderStatus from, SalesOrderStatus to) {
        return canTransition(from, to);
    }
}