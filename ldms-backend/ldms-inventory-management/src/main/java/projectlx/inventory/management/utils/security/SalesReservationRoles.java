package projectlx.inventory.management.utils.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum SalesReservationRoles {

    CREATE_SALES_RESERVATION("CREATE_SALES_RESERVATION", "Creates sales reservation"),
    DELETE_SALES_RESERVATION("DELETE_SALES_RESERVATION", "Deletes sales reservation"),
    UPDATE_SALES_RESERVATION("UPDATE_SALES_RESERVATION", "Updates sales reservation information"),
    VIEW_SALES_RESERVATION_BY_ID("VIEW_SALES_RESERVATION_BY_ID", "Views sales reservation by id"),
    VIEW_ALL_SALES_RESERVATIONS_AS_A_LIST("VIEW_ALL_SALES_RESERVATIONS_AS_A_LIST", "Views all sales reservations as a list"),
    VIEW_ALL_SALES_RESERVATIONS_BY_MULTIPLE_FILTERS("VIEW_ALL_SALES_RESERVATIONS_BY_MULTIPLE_FILTERS", "Views all sales reservations by multiple filters"),
    EXPORT_SALES_RESERVATIONS("EXPORT_SALES_RESERVATIONS", "Exports sales reservations"),
    IMPORT_SALES_RESERVATIONS("IMPORT_SALES_RESERVATIONS", "Imports sales reservations");

    private final String roleName;
    private final String description;
}
