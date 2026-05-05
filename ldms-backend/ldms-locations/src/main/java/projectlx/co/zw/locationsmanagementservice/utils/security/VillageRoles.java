package projectlx.co.zw.locationsmanagementservice.utils.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum VillageRoles {

    CREATE_VILLAGE("CREATE_VILLAGE", "Creates village"),
    UPDATE_VILLAGE("UPDATE_VILLAGE", "Updates village information"),
    VIEW_VILLAGE_BY_ID("VIEW_VILLAGE_BY_ID", "Views village by id"),
    DELETE_VILLAGE("DELETE_VILLAGE", "Deletes village"),
    VIEW_ALL_VILLAGES_AS_A_LIST("VIEW_ALL_VILLAGES_AS_A_LIST", "Views all villages as a list"),
    VIEW_ALL_VILLAGES_BY_MULTIPLE_FILTERS("VIEW_ALL_VILLAGES_BY_MULTIPLE_FILTERS", "Views all villages by multiple filters"),
    EXPORT_VILLAGES("EXPORT_VILLAGES", "Exports villages"),
    IMPORT_VILLAGES("IMPORT_VILLAGES", "Imports villages from CSV");

    private final String roleName;
    private final String description;
}
