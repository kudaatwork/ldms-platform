package projectlx.co.zw.locationsmanagementservice.utils.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum AdministrativeLevelRoles {

    CREATE_ADMINISTRATIVE_LEVEL("CREATE_ADMINISTRATIVE_LEVEL", "Creates administrative level"),
    UPDATE_ADMINISTRATIVE_LEVEL("UPDATE_ADMINISTRATIVE_LEVEL", "Updates administrative level information"),
    VIEW_ADMINISTRATIVE_LEVEL_BY_ID("VIEW_ADMINISTRATIVE_LEVEL_BY_ID", "Views administrative level by id"),
    DELETE_ADMINISTRATIVE_LEVEL("DELETE_ADMINISTRATIVE_LEVEL", "Deletes administrative level"),
    VIEW_ALL_ADMINISTRATIVE_LEVELS_AS_A_LIST("VIEW_ALL_ADMINISTRATIVE_LEVELS_AS_A_LIST", "Views all administrative levels as a list"),
    VIEW_ALL_ADMINISTRATIVE_LEVELS_BY_MULTIPLE_FILTERS("VIEW_ALL_ADMINISTRATIVE_LEVELS_BY_MULTIPLE_FILTERS", "Views all administrative levels by multiple filters"),
    EXPORT_ADMINISTRATIVE_LEVELS("EXPORT_ADMINISTRATIVE_LEVELS", "Exports administrative levels"),
    IMPORT_ADMINISTRATIVE_LEVELS("IMPORT_ADMINISTRATIVE_LEVELS", "Imports administrative levels from CSV");

    private final String roleName;
    private final String description;
}