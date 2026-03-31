package projectlx.co.zw.locationsmanagementservice.utils.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum SuburbRoles {

    CREATE_SUBURB("CREATE_SUBURB", "Creates suburb"),
    UPDATE_SUBURB("UPDATE_SUBURB", "Updates suburb information"),
    VIEW_SUBURB_BY_ID("VIEW_SUBURB_BY_ID", "Views suburb by id"),
    DELETE_SUBURB("DELETE_SUBURB", "Deletes suburb"),
    VIEW_ALL_SUBURBS_AS_A_LIST("VIEW_ALL_SUBURBS_AS_A_LIST", "Views all suburbs as a list"),
    VIEW_ALL_SUBURBS_BY_MULTIPLE_FILTERS("VIEW_ALL_SUBURBS_BY_MULTIPLE_FILTERS", "Views all suburbs by multiple filters"),
    EXPORT_SUBURBS("EXPORT_SUBURBS", "Exports suburbs"),
    IMPORT_SUBURBS("IMPORT_SUBURBS", "Imports suburbs from CSV");

    private final String roleName;
    private final String description;
}