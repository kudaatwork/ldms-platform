package projectlx.inventory.management.utils.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum StockAdjustmentRoles {

    CREATE_STOCK_ADJUSTMENT("CREATE_STOCK_ADJUSTMENT", "Creates stock adjustment"),
    DELETE_STOCK_ADJUSTMENT("DELETE_STOCK_ADJUSTMENT", "Deletes stock adjustment"),
    UPDATE_STOCK_ADJUSTMENT("UPDATE_STOCK_ADJUSTMENT", "Updates stock adjustment information"),
    VIEW_STOCK_ADJUSTMENT_BY_ID("VIEW_STOCK_ADJUSTMENT_BY_ID", "Views stock adjustment by id"),
    VIEW_ALL_STOCK_ADJUSTMENTS_AS_A_LIST("VIEW_ALL_STOCK_ADJUSTMENTS_AS_A_LIST", "Views all stock adjustments as a list"),
    VIEW_ALL_STOCK_ADJUSTMENTS_BY_MULTIPLE_FILTERS("VIEW_ALL_STOCK_ADJUSTMENTS_BY_MULTIPLE_FILTERS", "Views all stock adjustments by multiple filters"),
    EXPORT_STOCK_ADJUSTMENTS("EXPORT_STOCK_ADJUSTMENTS", "Exports stock adjustments"),
    IMPORT_STOCK_ADJUSTMENTS("IMPORT_STOCK_ADJUSTMENTS", "Imports stock adjustments");

    private final String roleName;
    private final String description;
}
